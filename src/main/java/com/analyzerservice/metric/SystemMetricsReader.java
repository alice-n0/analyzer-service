package com.analyzerservice.metric;

/**
 * 외부 관측 소스에서 latency·error rate를 읽어 옵니다. 테스트에서는 목 구현으로 대체합니다.
 */
public interface SystemMetricsReader {

    SystemMetrics read();
}
