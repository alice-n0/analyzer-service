package com.analyzerservice.detector;

/**
 * 임계값 기반 이상 감지 결과.
 */
public record AnomalyDetectionResult(
        boolean anomaly,
        double latencySeconds,
        double errorRate,
        boolean latencyExceeded,
        boolean errorRateExceeded,
        String summary) {}
