package com.analyzerservice.ai;

import com.analyzerservice.metric.SystemMetrics;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 프롬프트 생성은 {@link AnalysisPromptBuilder}에 위임하고, Claude 호출은 mock으로 둡니다.
 */
@Component
public class AIClient implements IncidentAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(AIClient.class);

    private final AnalysisPromptBuilder promptBuilder;

    public AIClient(AnalysisPromptBuilder promptBuilder) {
        this.promptBuilder = Objects.requireNonNull(promptBuilder, "promptBuilder");
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
        return mockClaudeCompletion(prompt);
    }

    /**
     * Claude Messages API 호출을 가정한 mock. 실제 HTTP/서명/스트리밍은 구현하지 않습니다.
     */
    private String mockClaudeCompletion(String prompt) {
        if (prompt == null) {
            log.warn("MOCK Claude: prompt가 null입니다.");
            return "[MOCK Claude 응답] 프롬프트 없음";
        }
        log.info("MOCK Claude 호출: promptLength={}", prompt.length());
        return "[MOCK Claude 응답] 실제 API 연동 시 프롬프트에 따라 원인 추정·근거 설명·해결 방법이 생성됩니다.";
    }
}
