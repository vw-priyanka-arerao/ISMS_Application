package vwg.cms.c4c.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import vwg.cms.c4c.config.LlmProperties;

/**
 * Thin client for the VW LLMaaS Chat Completions API (CloudIDP token + virtual key auth).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmChatService {

    private final LlmProperties llmProperties;
    private final LlmTokenService llmTokenService;
    private final RestClient.Builder restClientBuilder;

    public boolean isEnabled() {
        return llmProperties.enabled() && hasText(llmProperties.virtualKey());
    }

    public Optional<String> complete(String systemPrompt, String userPrompt) {
        if (!isEnabled()) {
            log.info("[LLM_CALL_SKIPPED] LLM disabled or not configured (app.ai.llm.enabled/virtual-key) - using heuristic fallback");
            return Optional.empty();
        }
        try {
            log.info("[LLM_CALL_ATTEMPT] Calling model '{}' at {}", llmProperties.model(), llmProperties.baseUrl());
            String accessToken = resolveAccessToken();
            Map<String, Object> requestBody = Map.of(
                    "model", llmProperties.model(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            ChatCompletionResponse response = restClientBuilder.build()
                    .post()
                    .uri(llmProperties.baseUrl() + "/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-LLM-API-CLIENT-ID", "Bearer " + llmProperties.virtualKey())
                    .body(requestBody)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                log.warn("[LLM_CALL_EMPTY] Model '{}' returned no choices - falling back to heuristic logic", llmProperties.model());
                return Optional.empty();
            }
            String content = response.choices().get(0).message().content();
            if (!hasText(content)) {
                log.warn("[LLM_CALL_EMPTY] Model '{}' returned blank content - falling back to heuristic logic", llmProperties.model());
                return Optional.empty();
            }
            log.info("[LLM_CALL_SUCCESS] Model '{}' responded with {} characters", llmProperties.model(), content.length());
            return Optional.of(content.trim());
        } catch (Exception ex) {
            log.warn("[LLM_CALL_FAILED] Model '{}' request failed ({}: {}) - falling back to heuristic logic",
                    llmProperties.model(), ex.getClass().getSimpleName(), ex.getMessage());
            return Optional.empty();
        }
    }

    // CloudIDP client credentials are optional: without them the virtual key is used as the bearer token directly.
    private String resolveAccessToken() {
        if (hasText(llmProperties.clientId()) && hasText(llmProperties.clientSecret())) {
            return llmTokenService.getAccessToken();
        }
        return llmProperties.virtualKey();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String role, String content) {
    }
}
