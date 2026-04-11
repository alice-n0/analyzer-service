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
        sb.append("현재 시스템에서 장애 발생\n\n");
        sb.append("메트릭:\n");
        sb.append("- latency: ").append(formatMetric(metrics.latencySeconds())).append('\n');
        sb.append("- errorRate: ").append(formatMetric(metrics.errorRate())).append("\n\n");
        sb.append("로그:\n");
        if (lines.isEmpty()) {
            sb.append("(로그 없음)\n");
        } else {
            for (String line : lines) {
                sb.append(line).append('\n');
            }
        }
        sb.append("\n요구:\n");
        sb.append("1. 원인 추정\n");
        sb.append("2. 근거 설명\n");
        sb.append("3. 해결 방법\n");
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
        return List.copyOf(out);
    }

    private static String formatMetric(double v) {
        if (!Double.isFinite(v)) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%g", v);
    }
}
