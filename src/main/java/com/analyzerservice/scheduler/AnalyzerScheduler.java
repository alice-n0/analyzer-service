package com.analyzerservice.scheduler;

import com.analyzerservice.ai.IncidentAnalyzer;
import com.analyzerservice.config.GrafanaAnnotationClient;
import com.analyzerservice.detector.AnomalyDetectionResult;
import com.analyzerservice.detector.AnomalyDetector;
import com.analyzerservice.log.ErrorLogSource;
import com.analyzerservice.metric.SystemMetrics;
import com.analyzerservice.metric.SystemMetricsReader;
import com.analyzerservice.notification.NotificationService;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주기적으로 메트릭을 조회하고, 이상 시 로그 수집 및 AI 분석을 오케스트레이션합니다.
 */
@Component
public class AnalyzerScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnalyzerScheduler.class);

    private static final String MDC_CYCLE_ID = "cycleId";

    private final SystemMetricsReader systemMetricsReader;
    private final AnomalyDetector anomalyDetector;
    private final ErrorLogSource errorLogSource;
    private final IncidentAnalyzer incidentAnalyzer;
    private final NotificationService notificationService;
    private final GrafanaAnnotationClient annotationClient;

    public AnalyzerScheduler(
            SystemMetricsReader systemMetricsReader,
            AnomalyDetector anomalyDetector,
            ErrorLogSource errorLogSource,
            IncidentAnalyzer incidentAnalyzer,
            NotificationService notificationService,
            GrafanaAnnotationClient annotationClient) {

        this.systemMetricsReader = systemMetricsReader;
        this.anomalyDetector = anomalyDetector;
        this.errorLogSource = errorLogSource;
        this.incidentAnalyzer = incidentAnalyzer;
        this.notificationService = notificationService;
        this.annotationClient = annotationClient;
    }

    @Scheduled(fixedDelay = 60_000)
    public void runAnalysisCycle() {
        String cycleId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(MDC_CYCLE_ID, cycleId);
        try {
            log.debug("분석 사이클 시작");
            executeCycle();
            log.debug("분석 사이클 정상 종료");
        } catch (RuntimeException e) {
            log.error("분석 사이클 실패: cycleId={}", cycleId, e);
            throw e;
        } finally {
            MDC.remove(MDC_CYCLE_ID);
        }
    }

    private void executeCycle() {
        log.info("[1/4] latency 조회 시작");
        SystemMetrics metrics = systemMetricsReader.read();
        log.info("RPS={}", metrics.rps());

        if (metrics.isNoData()) {
            log.warn("[STATE] NO_DATA - 트래픽 없음 또는 Prometheus 데이터 없음");
            return;
        }

        log.info(
                "[1/4] latency 조회 완료: latencySeconds={} (Prometheus p95 PromQL)",
                metrics.latencySeconds());

        log.info("[2/4] error rate 조회 완료: errorRate={}", metrics.errorRate());

        log.info("p99Latency={}", metrics.p99LatencySeconds());
        log.info("dbSaturation={}", metrics.dbSaturation());
        log.info("error5xxRate={}", metrics.error5xxRate());
        log.info("error4xxRate={}", metrics.error4xxRate());

        log.info(
                "[3/4] anomaly 판단 시작 (임계: latency>{}, errorRate>{})",
                AnomalyDetector.LATENCY_THRESHOLD, AnomalyDetector.ERROR_RATE_THRESHOLD);
        if (metrics.dbSaturation() > 0.8) {
            log.warn("[CAUSE] DB 포화 가능성");
        }

        if (metrics.error5xxRate() > 0.05) {
            log.warn("[CAUSE] 서버 내부 오류 증가");
        }

        if (metrics.error4xxRate() > 0.2) {
            log.warn("[CAUSE] 클라이언트 요청 문제 가능성");
        }

        AnomalyDetectionResult result = anomalyDetector.evaluate(metrics);
        log.info("[3/4] anomaly 판단 완료: anomaly={}, detail={}", result.anomaly(), result.summary());

        if (!result.anomaly()) {
            log.info("[STATE] NORMAL");
            log.info("[4/4] 정상 — 로그 수집·AI 호출 생략");
            return;
        }

        log.warn("[STATE] ANOMALY");
        log.warn("[4/4] anomaly 발생 — 후속 처리 시작: {}", result.summary());

        log.info("[4/4-a] 로그 수집 시작");
        List<String> errors = safeCollectLogs();
        log.info("[4/4-a] 로그 수집 완료: lineCount={}", errors.size());
        if (log.isDebugEnabled()) {
            for (int i = 0; i < errors.size(); i++) {
                log.debug("[4/4-a] log[{}]={}", i, errors.get(i));
            }
        }

        log.info("[4/4-b] IncidentAnalyzer 호출 시작");
        String analysis = incidentAnalyzer.analyzeIncident(metrics, errors);
        int responseLen = analysis != null ? analysis.length() : 0;
        log.info("[4/4-b] IncidentAnalyzer 호출 완료: responseLength={}", responseLen);
        String summary = extractSummary(analysis);
        log.info("[4/4-d] Grafana annotation 호출: summary={}", summary);
        annotationClient.createAnnotation(summary);
        notificationService.send(summary);

        if (analysis != null) {
            log.info("[4/4-c] AI 분석 결과 출력:\n{}", analysis);
        } else {
            log.warn("[4/4-c] AI 분석 결과가 null입니다.");
        }
    }

    private String extractSummary(String analysis) {
        if (analysis == null) {
            return "[AI 분석 결과 없음]";
        }

        String trimmed = analysis.trim();
        if (trimmed.isEmpty()) {
            return "[AI 분석 결과 없음]";
        }

        String[] lines = trimmed.split("\\R");
        String firstLine = lines[0].trim();
        String secondLine = lines.length > 1 ? lines[1].trim() : "";
        String combined = secondLine.isEmpty() ? firstLine : firstLine + " " + secondLine;
        if (combined.isEmpty()) {
            combined = "[AI 분석 결과 없음]";
        }
        if (combined.length() > 100) {
            return combined.substring(0, 100);
        }
        return combined;
    }

    private List<String> safeCollectLogs() {
        try {
            List<String> raw = errorLogSource.collectRecentErrors();
            if (raw == null) {
                log.warn("errorLogSource가 null 목록을 반환했습니다. 빈 목록으로 대체합니다.");
                return List.of();
            }
            return List.copyOf(raw);
        } catch (RuntimeException e) {
            log.error("로그 수집 중 예외 — 빈 목록으로 진행", e);
            return List.of();
        }
    }
}
