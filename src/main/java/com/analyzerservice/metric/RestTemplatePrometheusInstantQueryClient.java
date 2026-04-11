package com.analyzerservice.metric;

import com.analyzerservice.config.PrometheusProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@code GET /api/v1/query} 호출 및 응답 파싱만 담당합니다.
 */
@Component
public class RestTemplatePrometheusInstantQueryClient implements PrometheusInstantQueryClient {

    private static final Logger log = LoggerFactory.getLogger(RestTemplatePrometheusInstantQueryClient.class);

    private final RestTemplate restTemplate;
    private final PrometheusProperties prometheusProperties;

    public RestTemplatePrometheusInstantQueryClient(
            RestTemplate restTemplate, PrometheusProperties prometheusProperties) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate");
        this.prometheusProperties = Objects.requireNonNull(prometheusProperties, "prometheusProperties");
    }

    @Override
    public double queryInstantScalar(String promql) {
        if (promql == null || promql.isBlank()) {
            log.warn("PromQL이 비어 있어 조회를 건너뜁니다.");
            return 0.0;
        }

        promql = promql.replace("\n", " ").trim();

        String base = trimTrailingSlash(prometheusProperties.getUrl());
        if (base.isEmpty()) {
            log.warn("prometheus.url이 설정되지 않아 조회를 건너뜁니다.");
            return 0.0;
        }

        String encoded = URLEncoder.encode(promql, StandardCharsets.UTF_8);

        URI uri = UriComponentsBuilder
            .fromUriString(base + "/api/v1/query")
            .queryParam("query", encoded)
            .build(true)   // 이미 인코딩된 값
            .toUri();

        try {
            PrometheusInstantQueryResponse body = restTemplate.getForObject(uri, PrometheusInstantQueryResponse.class);
            return parseScalarSample(uri, promql, body);
        } catch (RestClientException e) {
            log.error("Prometheus HTTP 조회 실패: uri={}", uri, e);
            return 0.0;
        } catch (Exception e) {
            log.error("Prometheus 응답 처리 실패: uri={}", uri, e);
            return 0.0;
        }
    }

    private static double parseScalarSample(URI uri, String promql, PrometheusInstantQueryResponse body) {
        if (body == null || !"success".equalsIgnoreCase(body.getStatus()) || body.getData() == null) {
            log.warn("Prometheus 응답 비정상: statusEmptyOrNotSuccess, uri={}", uri);
            return 0.0;
        }

        List<PrometheusInstantQueryResponse.Result> results = body.getData().getResult();
        if (results == null || results.isEmpty()) {
            log.debug("Prometheus 쿼리 결과 없음: promqlSnippet={}", abbreviate(promql));
            return 0.0;
        }

        PrometheusInstantQueryResponse.Result first = results.get(0);
        if (first == null) {
            return 0.0;
        }

        List<Object> valueTuple = first.getValue();
        if (valueTuple == null || valueTuple.size() < 2) {
            log.debug("Prometheus value 튜플 형식 아님: uri={}", uri);
            return 0.0;
        }

        Object sample = valueTuple.get(1);
        if (sample == null) {
            return 0.0;
        }

        try {
            double v = Double.parseDouble(sample.toString());
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return 0.0;
            }
            return v;
        } catch (NumberFormatException e) {
            log.warn("Prometheus 샘플 파싱 실패: raw={}, uri={}", sample, uri);
            return 0.0;
        }
    }

    private static String abbreviate(String promql) {
        if (promql.length() <= 80) {
            return promql;
        }
        return promql.substring(0, 77) + "...";
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String t = url.trim();
        return t.endsWith("/") ? t.substring(0, t.length() - 1) : t;
    }
}
