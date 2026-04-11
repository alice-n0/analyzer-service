package com.analyzerservice.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "loki")
public class LokiProperties {

    /**
     * Loki 베이스 URL (Kubernetes 등에서는 서비스 DNS 사용)
     */
    @NotBlank
    private String url;

    /**
     * {@code /loki/api/v1/query_range}에 전달할 LogQL
     */
    @NotBlank
    private String errorLogQuery;

    /**
     * Loki HTTP API 경로 (베이스 URL에 이어 붙임)
     */
    @NotBlank
    private String queryRangePath;

    @Min(1)
    @Max(86400)
    private int lookbackSeconds;

    @Min(1)
    @Max(500)
    private int maxLines;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getErrorLogQuery() {
        return errorLogQuery;
    }

    public void setErrorLogQuery(String errorLogQuery) {
        this.errorLogQuery = errorLogQuery;
    }

    public String getQueryRangePath() {
        return queryRangePath;
    }

    public void setQueryRangePath(String queryRangePath) {
        this.queryRangePath = queryRangePath;
    }

    public int getLookbackSeconds() {
        return lookbackSeconds;
    }

    public void setLookbackSeconds(int lookbackSeconds) {
        this.lookbackSeconds = lookbackSeconds;
    }

    public int getMaxLines() {
        return maxLines;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }
}
