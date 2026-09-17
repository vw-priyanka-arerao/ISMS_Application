package vwg.cms.c4c.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import vwg.cms.c4c.config.LlmProperties;

/**
 * Fetches and caches CloudIDP access tokens (client-credentials grant) used to call the VW LLMaaS API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmTokenService {

    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 60;

    private final LlmProperties llmProperties;
    private final RestClient.Builder restClientBuilder;

    private final ReentrantLock lock = new ReentrantLock();
    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiresAt = Instant.EPOCH;

    public String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(cachedTokenExpiresAt)) {
            return cachedToken;
        }
        lock.lock();
        try {
            if (cachedToken != null && Instant.now().isBefore(cachedTokenExpiresAt)) {
                return cachedToken;
            }
            return fetchToken();
        } finally {
            lock.unlock();
        }
    }

    private String fetchToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", llmProperties.clientId());
        form.add("client_secret", llmProperties.clientSecret());
        form.add("grant_type", "client_credentials");

        TokenResponse response = restClientBuilder.build()
                .post()
                .uri(llmProperties.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("CloudIDP did not return an access token");
        }
        long expiresIn = response.expiresIn() == null ? 1800L : response.expiresIn();
        cachedToken = response.accessToken();
        cachedTokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - EXPIRY_SAFETY_MARGIN_SECONDS));
        return cachedToken;
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("token_type") String tokenType
    ) {
    }
}
