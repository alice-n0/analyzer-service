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
        String prompt = builder.build(new SystemMetrics(0.12, 0.03, 0.0, true), List.of("a", "b"));
        assertThat(prompt).contains("현재 시스템에서 장애 발생");
        assertThat(prompt).contains("- latency:");
        assertThat(prompt).contains("- errorRate:");
        assertThat(prompt).contains("a");
        assertThat(prompt).contains("b");
        assertThat(prompt).contains("1. 원인 추정");
    }

    @Test
    void build_nullLogsTreatedAsEmpty() {
        String prompt = builder.build(new SystemMetrics(0.1, 0.02, 0.0, true), null);
        assertThat(prompt).contains("(로그 없음)");
    }

    @Test
    void build_skipsNullAndBlankLines() {
        String prompt =
                builder.build(new SystemMetrics(0.1, 0.02, 0.0, true), Arrays.asList(" ok ", null, "", "tail"));
        assertThat(prompt).contains("ok");
        assertThat(prompt).contains("tail");
        assertThat(prompt).doesNotContain("null");
    }

    @Test
    void build_nonFiniteMetricsShowsNA() {
        String prompt = builder.build(new SystemMetrics(Double.NaN, 0.02, 0.0, true), List.of());
        assertThat(prompt).contains("N/A");
    }

    @Test
    void build_rejectsNullMetrics() {
        assertThatThrownBy(() -> builder.build(null, List.of())).isInstanceOf(NullPointerException.class);
    }
}
