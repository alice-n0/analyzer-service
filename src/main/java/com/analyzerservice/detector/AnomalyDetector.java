package com.analyzerservice.detector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.analyzerservice.metric.SystemMetrics;

/**
 * 레이턴시(초)와 에러율을 고정 임계값과 비교해 이상 여부를 판단합니다.
 */
@Component
public class AnomalyDetector {

    public static final double LATENCY_THRESHOLD = 0.5;
    public static final double ERROR_RATE_THRESHOLD = 0.05;

    /**
     * @param latency   지연(초). 유한값이고 {@code > LATENCY_THRESHOLD}이면 이상 후보.
     * @param errorRate 에러율(0~1). 유한값이고 {@code > ERROR_RATE_THRESHOLD}이면 이상 후보.
     * @return 둘 중 하나라도 임계값을 넘으면 {@code true} (비유한 입력은 임계 초과로 보지 않음)
     */
    public boolean isAnomaly(double latency, double errorRate) {
        return isLatencyExceeded(latency) || isErrorRateExceeded(errorRate);
    }

    /**
     * 어떤 조건이 임계값을 넘었는지 한국어로 설명합니다. 이상이 없으면 정상 메시지를 반환합니다.
     */
    public String describeAnomalyReason(double latency, double errorRate) {
        List<String> parts = new ArrayList<>();
        if (isLatencyExceeded(latency)) {
            parts.add(String.format("latency(%.4f초)가 임계값(%.2f초) 초과", latency, LATENCY_THRESHOLD));
        }
        if (isErrorRateExceeded(errorRate)) {
            parts.add(String.format("errorRate(%.4f)가 임계값(%.2f) 초과", errorRate, ERROR_RATE_THRESHOLD));
        }
        if (!parts.isEmpty()) {
            return String.join("; ", parts);
        }
        String base = String.format(
                "이상 없음: latency≤%.2f초, errorRate≤%.2f", LATENCY_THRESHOLD, ERROR_RATE_THRESHOLD);
        if (!Double.isFinite(latency) || !Double.isFinite(errorRate)) {
            return base + " (참고: 일부 메트릭이 비유한 값)";
        }
        return base;
    }

    /**
     * 스케줄러 등에서 임계 비교를 한 곳에서만 하도록 {@link AnomalyDetectionResult}를 만듭니다.
     */
    public AnomalyDetectionResult evaluate(SystemMetrics m) {
        Objects.requireNonNull(m, "metrics");

        boolean errorAnomaly = isErrorRateExceeded(m.errorRate()) || m.error5xxRate() > 0.05;
        boolean dbAnomaly = m.dbSaturation() > 0.8;

        boolean latencyAnomaly = isLatencyExceeded(m.latencySeconds()) && m.rps() > 20; // 🔥 핵심

        boolean anomaly = errorAnomaly || dbAnomaly || latencyAnomaly;

        String summary = buildSummary(m, errorAnomaly, dbAnomaly, latencyAnomaly);

        return new AnomalyDetectionResult(
                anomaly,
                m.latencySeconds(),
                m.errorRate(),
                latencyAnomaly,
                errorAnomaly,
                summary);
    }

    private String buildSummary(SystemMetrics m,
            boolean error,
            boolean db,
            boolean latency) {

        List<String> parts = new ArrayList<>();

        if (error) {
            parts.add("에러율 증가");
        }

        if (db) {
            parts.add("DB 포화");
        }

        if (latency) {
            parts.add("latency 증가 (트래픽 존재)");
        }

        if (parts.isEmpty()) {
            return "정상 범위";
        }

        return String.join(", ", parts);
    }

    private static boolean isLatencyExceeded(double latency) {
        return Double.isFinite(latency) && latency > LATENCY_THRESHOLD;
    }

    private static boolean isErrorRateExceeded(double errorRate) {
        return Double.isFinite(errorRate) && errorRate > ERROR_RATE_THRESHOLD;
    }
}
