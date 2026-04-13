package com.analyzerservice.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.analyzerservice.metric.SystemMetrics;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisPromptBuilderTest {

    private final AnalysisPromptBuilder builder = new AnalysisPromptBuilder();

    @Test
    void build_includesMetricsAndRequirements() {
        String prompt = builder.build(new SystemMetrics(0.12, 0.03, 0.0, true, 0.0, 0.0, 0.0, 0.0), List.of("a", "b"));
        assertThat(prompt).contains("metrics:");
        assertThat(prompt).contains("* latency:");
        assertThat(prompt).contains("* p99Latency:");
        assertThat(prompt).contains("* errorRate:");
        assertThat(prompt).contains("* error5xxRate:");
        assertThat(prompt).contains("* error4xxRate:");
        assertThat(prompt).contains("* rps:");
        assertThat(prompt).contains("* dbSaturation:");
        assertThat(prompt).contains("* a");
        assertThat(prompt).contains("* b");
        assertThat(prompt).contains("* 장애 원인");
        assertThat(prompt).contains("* 영향 범위");
        assertThat(prompt).contains("* 추정 원인 (DB / 코드 / 트래픽 / 외부 의존성)");
        assertThat(prompt).contains("* 대응 방법");
    }

    @Test
    void build_nullLogsTreatedAsEmpty() {
        String prompt = builder.build(new SystemMetrics(0.1, 0.02, 0.0, true, 0.0, 0.0, 0.0, 0.0), null);
        assertThat(prompt).contains("(로그 없음)");
    }

    @Test
    void build_skipsNullAndBlankLines() {
        String prompt =
                builder.build(new SystemMetrics(0.1, 0.02, 0.0, true, 0.0, 0.0, 0.0, 0.0), Arrays.asList(" ok ", null, "", "tail"));
        assertThat(prompt).contains("* ok");
        assertThat(prompt).contains("* tail");
        assertThat(prompt).doesNotContain("null");
    }

    @Test
    void build_limitsLogLinesTo20() {
        List<String> logs = java.util.stream.IntStream.rangeClosed(1, 25)
                .mapToObj(i -> "line-" + i)
                .toList();
        String prompt = builder.build(new SystemMetrics(0.1, 0.02, 0.0, true, 0.0, 0.0, 0.0, 0.0), logs);
        assertThat(prompt).contains("* line-1");
        assertThat(prompt).contains("* line-20");
        assertThat(prompt).doesNotContain("* line-21");
    }

    @Test
    void build_nonFiniteMetricsShowsNA() {
        String prompt = builder.build(new SystemMetrics(Double.NaN, 0.02, 0.0, true, 0.0, 0.0, 0.0, 0.0), List.of());
        assertThat(prompt).contains("N/A");
    }

    @Test
    void build_rejectsNullMetrics() {
        assertThatThrownBy(() -> builder.build(null, List.of())).isInstanceOf(NullPointerException.class);
    }
}
