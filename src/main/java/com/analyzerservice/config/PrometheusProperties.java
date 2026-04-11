package com.analyzerservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "prometheus")
public class PrometheusProperties {

    /**
     * Prometheus 서버 베이스 URL (Kubernetes 등에서는 서비스 DNS 사용)
     */
    @NotBlank
    private String url;

    @Valid
    @NotNull
    private Queries queries = new Queries();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Queries getQueries() {
        return queries;
    }

    public void setQueries(Queries queries) {
        this.queries = queries;
    }

    public static class Queries {

        /**
         * p95 레이턴시(초 단위 스칼라)를 반환하는 PromQL.
         */
        @NotBlank
        private String p95Latency;

        /**
         * 에러 비율(0~1 스칼라)을 반환하는 PromQL.
         */
        @NotBlank
        private String errorRate;

        public String getP95Latency() {
            return p95Latency;
        }

        public void setP95Latency(String p95Latency) {
            this.p95Latency = p95Latency;
        }

        public String getErrorRate() {
            return errorRate;
        }

        public void setErrorRate(String errorRate) {
            this.errorRate = errorRate;
        }
    }
}
