package com.analyzerservice.metric;

import com.analyzerservice.config.PrometheusProperties;

import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PrometheusSystemMetricsReader implements SystemMetricsReader {

    private static final Logger log = LoggerFactory.getLogger(PrometheusSystemMetricsReader.class);

    // service 라벨 적용
    private static final String RPS_PROMQL =
            "sum(rate(http_server_requests_seconds_count{service=\"$service\"}[1m]))";

    private static final String LATENCY_PROMQL =
            "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{service=\"$service\"}[1m])) by (le))";

    private static final String LATENCY_P99_PROMQL =
            "histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket{service=\"$service\"}[1m])) by (le))";

    private static final String ERROR_RATE_PROMQL =
            "sum(rate(http_server_requests_seconds_count{service=\"$service\", status=~\"5..\"}[1m]))"
                    + " / sum(rate(http_server_requests_seconds_count{service=\"$service\"}[1m]))";

    private static final String ERROR_4XX_RATE_PROMQL =
            "sum(rate(http_server_requests_seconds_count{service=\"$service\", status=~\"4..\"}[1m]))"
                    + " / sum(rate(http_server_requests_seconds_count{service=\"$service\"}[1m]))";

    private static final String DB_SATURATION_PROMQL =
            "hikaricp_connections_active{service=\"$service\"} / hikaricp_connections_max{service=\"$service\"}";

    private final PrometheusInstantQueryClient queryClient;
    private final PrometheusProperties prometheusProperties;

    public PrometheusSystemMetricsReader(
            PrometheusInstantQueryClient queryClient,
            PrometheusProperties prometheusProperties) {
        this.queryClient = Objects.requireNonNull(queryClient, "queryClient");
        this.prometheusProperties = Objects.requireNonNull(prometheusProperties, "prometheusProperties");
    }
    @Override
    public List<String> getAvailableServices() {
        return queryClient.queryLabelValues(
            "http_server_requests_seconds_count",
            "service"
        );
    }
    
    @Override
    public SystemMetrics read(String serviceName) {
        PrometheusProperties.Queries queries = prometheusProperties.getQueries();

        if (queries == null) {
            log.warn("[{}] prometheus.queries가 null", serviceName);
            return empty(serviceName);
        }

        double rps = query(RPS_PROMQL, serviceName);

        if (rps == 0.0) {
            log.debug("[{}] RPS=0 → 조회 생략", serviceName);
            return empty(serviceName);
        }

        double latencySeconds = query(LATENCY_PROMQL, serviceName);
        double p99LatencySeconds = query(LATENCY_P99_PROMQL, serviceName);
        double errorRate = query(ERROR_RATE_PROMQL, serviceName);
        double error4xxRate = query(ERROR_4XX_RATE_PROMQL, serviceName);
        double dbSaturation = query(DB_SATURATION_PROMQL, serviceName);

        log.debug(
                "[{}] latency={}, p99={}, errorRate={}, error4xx={}, rps={}, dbSat={}",
                serviceName,
                latencySeconds,
                p99LatencySeconds,
                errorRate,
                error4xxRate,
                rps,
                dbSaturation
        );

        return new SystemMetrics(
                serviceName,
                latencySeconds,
                errorRate,
                rps,
                true,
                p99LatencySeconds,
                dbSaturation,
                errorRate,      // 5xx rate
                error4xxRate
        );
    }

    // =========================
    // helper
    // =========================

    private double query(String promql, String serviceName) {
        String resolved = promql.replace("$service", serviceName);
        return queryClient.queryInstantScalar(resolved);
    }

    private SystemMetrics empty(String serviceName) {
        return new SystemMetrics(serviceName, 0, 0, 0, false, 0, 0, 0, 0);
    }
}
