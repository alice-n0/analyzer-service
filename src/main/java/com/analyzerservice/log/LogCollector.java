package com.analyzerservice.log;

import com.analyzerservice.config.LokiProperties;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

/**
 * Loki {@code query_range}로 설정된 시간 범위·LogQL에 따라 에러 로그를 조회합니다.
 */
@Component
public class LogCollector implements ErrorLogSource {

    private static final Logger log = LoggerFactory.getLogger(LogCollector.class);

    private final RestTemplate restTemplate;
    private final LokiProperties lokiProperties;

    public LogCollector(RestTemplate restTemplate, LokiProperties lokiProperties) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate");
        this.lokiProperties = Objects.requireNonNull(lokiProperties, "lokiProperties");
    }

    @Override
    public List<String> collectRecentErrors() {
        String base = trimTrailingSlash(lokiProperties.getUrl());
        if (base.isEmpty()) {
            log.warn("loki.url이 설정되지 않아 로그 조회를 건너뜁니다.");
            return List.of();
        }

        String logQl = lokiProperties.getErrorLogQuery();
        if (logQl == null || logQl.isBlank()) {
            log.warn("loki.error-log-query가 비어 있어 로그 조회를 건너뜁니다.");
            return List.of();
        }

        int lookback = lokiProperties.getLookbackSeconds();
        int maxLines = lokiProperties.getMaxLines();

        Instant end = Instant.now();
        Instant start = end.minusSeconds(lookback);
        long startNs = toEpochNanos(start);
        long endNs = toEpochNanos(end);

        String query =
                "query="
                        + UriUtils.encodeQueryParam(logQl, StandardCharsets.UTF_8)
                        + "&start="
                        + UriUtils.encodeQueryParam(Long.toString(startNs), StandardCharsets.UTF_8)
                        + "&end="
                        + UriUtils.encodeQueryParam(Long.toString(endNs), StandardCharsets.UTF_8)
                        + "&limit="
                        + UriUtils.encodeQueryParam(Integer.toString(maxLines), StandardCharsets.UTF_8);
        String path = lokiProperties.getQueryRangePath();
        if (path == null || path.isBlank()) {
            log.warn("loki.query-range-path가 비어 있어 로그 조회를 건너뜁니다.");
            return List.of();
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        URI uri = URI.create(base + path + "?" + query);

        try {
            LokiQueryRangeResponse body = restTemplate.getForObject(uri, LokiQueryRangeResponse.class);
            return parseLogLines(uri, body, maxLines);
        } catch (RestClientException e) {
            log.error("Loki HTTP 조회 실패: uri={}", uri, e);
            return List.of();
        } catch (Exception e) {
            log.error("Loki 응답 처리 실패: uri={}", uri, e);
            return List.of();
        }
    }

    private static List<String> parseLogLines(URI uri, LokiQueryRangeResponse body, int maxLines) {
        if (body == null || !"success".equalsIgnoreCase(body.getStatus()) || body.getData() == null) {
            log.warn("Loki 응답 비정상: uri={}", uri);
            return List.of();
        }

        List<LokiQueryRangeResponse.LokiStreamResult> streams = body.getData().getResult();
        if (streams == null || streams.isEmpty()) {
            log.debug("Loki query_range 결과 스트림 없음: uri={}", uri);
            return List.of();
        }

        List<TimestampedLine> lines = new ArrayList<>();
        for (LokiQueryRangeResponse.LokiStreamResult stream : streams) {
            if (stream == null || stream.getValues() == null) {
                continue;
            }
            for (List<String> pair : stream.getValues()) {
                if (pair == null || pair.size() < 2) {
                    continue;
                }
                String ts = pair.get(0);
                String line = pair.get(1);
                if (line == null) {
                    continue;
                }
                long ns = parseNanos(ts);
                lines.add(new TimestampedLine(ns, line));
            }
        }

        lines.sort(Comparator.comparingLong(TimestampedLine::timestampNs).reversed());

        List<String> out = new ArrayList<>(Math.min(maxLines, lines.size()));
        for (int i = 0; i < lines.size() && out.size() < maxLines; i++) {
            out.add(lines.get(i).line());
        }
        return List.copyOf(out);
    }

    private static long parseNanos(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static long toEpochNanos(Instant instant) {
        return instant.getEpochSecond() * 1_000_000_000L + instant.getNano();
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String t = url.trim();
        return t.endsWith("/") ? t.substring(0, t.length() - 1) : t;
    }

    private record TimestampedLine(long timestampNs, String line) {}
}
