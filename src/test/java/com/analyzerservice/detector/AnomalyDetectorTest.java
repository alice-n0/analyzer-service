package com.analyzerservice.detector;

import static org.assertj.core.api.Assertions.assertThat;

import com.analyzerservice.metric.SystemMetrics;
import org.junit.jupiter.api.Test;

class AnomalyDetectorTest {

    private final AnomalyDetector detector = new AnomalyDetector();

    @Test
    void isAnomaly_falseWhenBelowThresholds() {
        assertThat(detector.isAnomaly(0.1, 0.01)).isFalse();
    }

    @Test
    void isAnomaly_trueWhenLatencyHigh() {
        assertThat(detector.isAnomaly(0.6, 0.01)).isTrue();
    }

    @Test
    void isAnomaly_trueWhenErrorRateHigh() {
        assertThat(detector.isAnomaly(0.1, 0.06)).isTrue();
    }

    @Test
    void isAnomaly_falseForNonFiniteValues() {
        assertThat(detector.isAnomaly(Double.NaN, Double.NaN)).isFalse();
        assertThat(detector.isAnomaly(Double.POSITIVE_INFINITY, 0.0)).isFalse();
    }

    @Test
    void evaluate_matchesThresholdFlags() {
        AnomalyDetectionResult r = detector.evaluate(new SystemMetrics(0.6, 0.02, 0.0, true));
        assertThat(r.anomaly()).isTrue();
        assertThat(r.latencyExceeded()).isTrue();
        assertThat(r.errorRateExceeded()).isFalse();
    }

    @Test
    void describeReason_notesNonFiniteWhenNoThresholdBreach() {
        String s = detector.describeAnomalyReason(Double.NaN, 0.01);
        assertThat(s).contains("비유한");
    }
}
