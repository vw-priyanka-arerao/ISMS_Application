package vwg.cms.c4c.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.llm")
public record LlmProperties(
        boolean enabled,
        String tokenUrl,
        String clientId,
        String clientSecret,
        String baseUrl,
        String virtualKey,
        String model
) {
}
