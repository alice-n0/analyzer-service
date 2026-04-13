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
        sb.append("metrics:\n\n");
        sb.append("* latency: ").append(formatMetric(metrics.latencySeconds())).append('\n');
        sb.append("* p99Latency: ").append(formatMetric(metrics.p99LatencySeconds())).append('\n');
        sb.append("* errorRate: ").append(formatMetric(metrics.errorRate())).append('\n');
        sb.append("* error5xxRate: ").append(formatMetric(metrics.error5xxRate())).append('\n');
        sb.append("* error4xxRate: ").append(formatMetric(metrics.error4xxRate())).append('\n');
        sb.append("* rps: ").append(formatMetric(metrics.rps())).append('\n');
        sb.append("* dbSaturation: ").append(formatMetric(metrics.dbSaturation())).append("\n\n");
        sb.append("logs:\n");
        if (lines.isEmpty()) {
            sb.append("(로그 없음)\n");
        } else {
            for (String line : lines) {
                sb.append("* ").append(line).append('\n');
            }
        }
        sb.append("\n이 데이터를 기반으로 원인, 영향, 유형(DB/코드/트래픽/외부), 대응을 3줄 이내로 요약해줘.");

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
        int maxLines = 5;
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
