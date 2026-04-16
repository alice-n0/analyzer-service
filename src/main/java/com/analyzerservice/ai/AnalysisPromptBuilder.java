package com.analyzerservice.ai;

import com.analyzerservice.metric.SystemMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 장애 분석용 프롬프트 문자열만 조립합니다 (LLM 호출 없음).
 */
@Component
public class AnalysisPromptBuilder {

    public String build(SystemMetrics metrics, List<String> logs) {
        Objects.requireNonNull(metrics, "metrics");
        List<String> lines = sanitizeLogLines(logs);

        StringBuilder sb = new StringBuilder();
        sb.append("다음은 서비스 장애 데이터다.\n");

        sb.append("service: ").append(metrics.serviceName()).append('\n');

        sb.append("metrics:\n");
        sb.append("- latency: ").append(formatMetric(metrics.latencySeconds())).append('\n');
        sb.append("- p99: ").append(formatMetric(metrics.p99LatencySeconds())).append('\n');
        sb.append("- errorRate: ").append(formatMetric(metrics.errorRate())).append('\n');
        sb.append("- 5xx: ").append(formatMetric(metrics.error5xxRate())).append('\n');
        sb.append("- 4xx: ").append(formatMetric(metrics.error4xxRate())).append('\n');
        sb.append("- rps: ").append(formatMetric(metrics.rps())).append('\n');
        sb.append("- db: ").append(formatMetric(metrics.dbSaturation())).append('\n');

        sb.append("logs:\n");
        if (lines.isEmpty()) {
            sb.append("- 없음\n");
        } else {
            for (String line : lines) {
                sb.append("- ").append(line).append('\n');
            }
        }

        sb.append("\n");

        sb.append("다음 형식으로만 한국어로 답변:\n");
        sb.append("요약: \n");
        sb.append("영향: \n");
        sb.append("원인: \n");
        sb.append("대응: \n");

        return sb.toString();
    }

    private static List<String> sanitizeLogLines(List<String> logs) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String line : logs) {
            if (line == null) {
                continue;
            }
            String t = line.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        int maxLines = 7;
        if (out.size() <= maxLines) {
            return List.copyOf(out);
        }
        return List.copyOf(out.subList(0, maxLines));
    }

    private static String formatMetric(double v) {
        if (!Double.isFinite(v)) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%g", v);
    }
}
