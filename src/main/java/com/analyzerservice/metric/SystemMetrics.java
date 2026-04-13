package com.analyzerservice.metric;

/**
 * 한 사이클에서 조회한 시스템 메트릭
 */
public record SystemMetrics(
        double latencySeconds,
        double errorRate,
        double rps,
        boolean hasData,
        double p99LatencySeconds,
        double dbSaturation,
        double error5xxRate,
        double error4xxRate) {

    public SystemMetrics {
        latencySeconds = Double.isFinite(latencySeconds) ? latencySeconds : Double.NaN;
        errorRate = Double.isFinite(errorRate) ? errorRate : Double.NaN;
        rps = Double.isFinite(rps) ? rps : Double.NaN;
        p99LatencySeconds = Double.isFinite(p99LatencySeconds) ? p99LatencySeconds : Double.NaN;
        dbSaturation = Double.isFinite(dbSaturation) ? dbSaturation : Double.NaN;
        error5xxRate = Double.isFinite(error5xxRate) ? error5xxRate : Double.NaN;
        error4xxRate = Double.isFinite(error4xxRate) ? error4xxRate : Double.NaN;
    }

    public boolean isNoData() {
        return !hasData;
    }
}
