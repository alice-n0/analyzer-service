package com.analyzerservice.metric;

import com.analyzerservice.config.PrometheusProperties;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Prometheus에서 p95 지연·에러율 PromQL을 각각 실행해 {@link SystemMetrics}를 만듭니다.
 */
@Component
public class PrometheusSystemMetricsReader implements SystemMetricsReader {

    private static final Logger log = LoggerFactory.getLogger(PrometheusSystemMetricsReader.class);

    private final PrometheusInstantQueryClient queryClient;
    private final PrometheusProperties prometheusProperties;

    public PrometheusSystemMetricsReader(
            PrometheusInstantQueryClient queryClient, PrometheusProperties prometheusProperties) {
        this.queryClient = Objects.requireNonNull(queryClient, "queryClient");
        this.prometheusProperties = Objects.requireNonNull(prometheusProperties, "prometheusProperties");
    }

    @Override
    public SystemMetrics read() {
        PrometheusProperties.Queries queries = prometheusProperties.getQueries();
        if (queries == null) {
            log.warn("prometheus.queries가 null입니다. (0,0) 반환");
            return new SystemMetrics(0.0, 0.0);
        }

        String latencyQuery = queries.getP95Latency();
        String errorQuery = queries.getErrorRate();

        double latencySeconds = queryClient.queryInstantScalar(latencyQuery);
        double errorRate = queryClient.queryInstantScalar(errorQuery);

        log.debug("SystemMetrics 조회 완료: latencySeconds={}, errorRate={}", latencySeconds, errorRate);
        return new SystemMetrics(latencySeconds, errorRate);
    }
}
