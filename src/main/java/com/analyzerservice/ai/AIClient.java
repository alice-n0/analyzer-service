package com.analyzerservice.ai;

import com.analyzerservice.config.OpenAIProperties;
import com.analyzerservice.metric.SystemMetrics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 프롬프트 생성은 {@link AnalysisPromptBuilder}에 위임하고, OpenAI Chat Completions API를 호출합니다.
 */
@Component
public class AIClient implements IncidentAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(AIClient.class);
    private static final String OPENAI_CHAT_COMPLETIONS_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-4o-mini";
    private static final String SYSTEM_MESSAGE =
            "You are an expert SRE. Analyze metrics and logs to find root cause.";

    private final AnalysisPromptBuilder promptBuilder;
    private final RestTemplate restTemplate;
    private final OpenAIProperties openAIProperties;
    private final ObjectMapper objectMapper;

    public AIClient(
            AnalysisPromptBuilder promptBuilder,
            RestTemplate restTemplate,
            OpenAIProperties openAIProperties,
            ObjectMapper objectMapper) {
        this.promptBuilder = Objects.requireNonNull(promptBuilder, "promptBuilder");
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate");
        this.openAIProperties = Objects.requireNonNull(openAIProperties, "openAIProperties");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * {@link AnalysisPromptBuilder#build(SystemMetrics, List)}와 동일한 프롬프트를 반환합니다.
     */
    public String buildAnalysisPrompt(SystemMetrics metrics, List<String> logs) {
        return promptBuilder.build(metrics, logs);
    }

    @Override
    public String analyzeIncident(SystemMetrics metrics, List<String> errorLogs) {
        Objects.requireNonNull(metrics, "metrics");
        String prompt = promptBuilder.build(metrics, errorLogs);
        return requestOpenAICompletion(prompt);
    }

    private String requestOpenAICompletion(String prompt) {
        if (prompt == null) {
            log.warn("OpenAI 호출 생략: prompt가 null입니다.");
            return "[OpenAI 응답 없음] 프롬프트가 null입니다.";
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAIProperties.getApiKey());

            Map<String, Object> requestBody = Map.of(
                    "model", OPENAI_MODEL,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_MESSAGE),
                            Map.of("role", "user", "content", prompt)));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            String rawResponse = restTemplate.postForObject(
                    OPENAI_CHAT_COMPLETIONS_ENDPOINT, requestEntity, String.class);
            return extractMessageContent(rawResponse);
        } catch (RuntimeException e) {
            log.error("OpenAI Chat Completions 호출 실패", e);
            return "[OpenAI 호출 실패] " + e.getMessage();
        }
    }

    private String extractMessageContent(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            log.warn("OpenAI 응답이 비어 있습니다.");
            return "[OpenAI 응답 없음]";
        }
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.isNull()) {
                log.warn("OpenAI 응답에서 choices[0].message.content를 찾지 못했습니다.");
                return "[OpenAI 응답 파싱 실패] choices[0].message.content 없음";
            }
            return contentNode.asText();
        } catch (Exception e) {
            log.error("OpenAI 응답 JSON 파싱 실패", e);
            return "[OpenAI 응답 파싱 실패] " + e.getMessage();
        }
    }
}
