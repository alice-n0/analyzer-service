package com.analyzerservice.config;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GrafanaAnnotationClient {

    private final RestTemplate restTemplate;
    private final String grafanaUrl;
    private final String grafanaApiToken;

    public GrafanaAnnotationClient(
            RestTemplate restTemplate,
            @Value("${grafana.url}") String grafanaUrl,
            @Value("${grafana.apiKey}") String grafanaApiToken) {
        this.restTemplate = restTemplate;
        this.grafanaUrl = grafanaUrl;
        this.grafanaApiToken = grafanaApiToken;
    }

    public void createAnnotation(String summary) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(grafanaApiToken);

        Map<String, Object> body = Map.of(
                "text", summary,
                "tags", List.of("AI", "ANOMALY"),
                "time", System.currentTimeMillis());

        restTemplate.postForEntity(
                grafanaUrl + "/api/annotations",
                new HttpEntity<>(body, headers),
                Void.class);
    }
}
