package com.analyzerservice.metric;

import com.analyzerservice.config.PrometheusProperties;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Prometheus에서 RPS를 먼저 조회하고, 트래픽이 있을 때만 p95 지연·에러율을 조회해 {@link SystemMetrics}를 만듭니다.
 */
@Component
public class PrometheusSystemMetricsReader implements SystemMetricsReader {

    private static final Logger log = LoggerFactory.getLogger(PrometheusSystemMetricsReader.class);

    private static final String RPS_PROMQL = "sum(rate(http_server_requests_seconds_count[1m]))";

    private static final String LATENCY_PROMQL =
            "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[1m])) by (le))";

    private static final String ERROR_RATE_PROMQL =
            "sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[1m]))"
                    + " / sum(rate(http_server_requests_seconds_count[1m]))";

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
            return new SystemMetrics(0.0, 0.0, 0.0, false);
        }

        double rps = queryClient.queryInstantScalar(RPS_PROMQL);
        if (rps == 0.0) {
            log.debug("RPS가 0이어서 latency·errorRate 조회를 생략합니다.");
            return new SystemMetrics(0.0, 0.0, 0.0, false);
        }

        double latencySeconds = queryClient.queryInstantScalar(LATENCY_PROMQL);
        double errorRate = queryClient.queryInstantScalar(ERROR_RATE_PROMQL);

        log.debug("SystemMetrics 조회 완료: latencySeconds={}, errorRate={}, rps={}, hasData={}",
                latencySeconds, errorRate, rps, true);
        return new SystemMetrics(latencySeconds, errorRate, rps, true);
    }
}
