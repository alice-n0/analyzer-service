package com.analyzerservice.metric;

/**
 * 한 사이클에서 조회한 시스템 메트릭 스냅샷.
 */
public record SystemMetrics(double latencySeconds, double errorRate, double rps, boolean hasData) {

    public SystemMetrics {
        latencySeconds = Double.isFinite(latencySeconds) ? latencySeconds : Double.NaN;
        errorRate = Double.isFinite(errorRate) ? errorRate : Double.NaN;
        rps = Double.isFinite(rps) ? rps : Double.NaN;
    }

    public boolean isNoData() {
        return !hasData;
    }
}
