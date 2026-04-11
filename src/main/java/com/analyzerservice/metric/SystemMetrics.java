package com.analyzerservice.metric;

/**
 * 한 사이클에서 조회한 시스템 메트릭 스냅샷.
 */
public record SystemMetrics(double latencySeconds, double errorRate) {

    public SystemMetrics {
        latencySeconds = Double.isFinite(latencySeconds) ? latencySeconds : Double.NaN;
        errorRate = Double.isFinite(errorRate) ? errorRate : Double.NaN;
    }
}
