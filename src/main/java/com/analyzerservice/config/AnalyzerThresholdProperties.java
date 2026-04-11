package com.analyzerservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analyzer.thresholds")
public class AnalyzerThresholdProperties {

    /**
     * p95 레이턴시 임계값 (밀리초). 초과 시 이상으로 간주.
     */
    private double p95LatencyMs = 500.0;

    /**
     * 에러율 임계값 (0~1). 초과 시 이상으로 간주.
     */
    private double errorRate = 0.05;

    public double getP95LatencyMs() {
        return p95LatencyMs;
    }

    public void setP95LatencyMs(double p95LatencyMs) {
        this.p95LatencyMs = p95LatencyMs;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(double errorRate) {
        this.errorRate = errorRate;
    }
}
