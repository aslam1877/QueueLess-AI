package com.queueless.client;

import com.queueless.client.dto.PredictionRequest;
import com.queueless.client.dto.PredictionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP client for communicating with the external wait-time prediction microservice.
 */
@Component
public class PredictionClient {

    private static final Logger log = LoggerFactory.getLogger(PredictionClient.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final String baseUrl;

    public PredictionClient(
            @Value("${app.ml-service.url:http://localhost:8000}") String baseUrl,
            @Value("${app.ml-service.enabled:true}") boolean enabled,
            @Value("${app.ml-service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${app.ml-service.read-timeout-ms:3000}") int readTimeoutMs) {
        this.baseUrl = baseUrl;
        this.enabled = enabled;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        log.info("Initialized PredictionClient: target='{}', enabled={}, timeouts=[connect: {}ms, read: {}ms]",
                baseUrl, enabled, connectTimeoutMs, readTimeoutMs);
    }

    /**
     * Sends current queue parameters to Python FastAPI POST /predict.
     *
     * @param request payload containing queue features
     * @return Optional containing prediction if successful, or empty if service is offline/errored
     */
    public Optional<PredictionResponse> predictWaitTime(PredictionRequest request) {
        if (!enabled) {
            log.debug("Python ML service is disabled via configuration. Bypassing prediction call.");
            return Optional.empty();
        }

        try {
            PredictionResponse response = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(PredictionResponse.class);

            if (response != null) {
                log.debug("Received ML prediction: {} mins (baseline: {} mins, source: {})",
                        response.getPredictedWaitMinutes(),
                        response.getBaselineWaitMinutes(),
                        response.getEstimationSource());
                return Optional.of(response);
            }
        } catch (ResourceAccessException e) {
            log.warn("Cannot reach Python ML service at {} ({}). Failing over to deterministic baseline.",
                    baseUrl, e.getMessage());
        } catch (RestClientResponseException e) {
            log.warn("Python ML service returned HTTP {} ({}). Failing over to deterministic baseline.",
                    e.getStatusCode(), e.getStatusText());
        } catch (Exception e) {
            log.error("Unexpected error calling Python ML service. Failing over to deterministic baseline.", e);
        }

        return Optional.empty();
    }

    /**
     * Checks if the external ML service is currently responsive and reports UP.
     */
    public boolean isHealthy() {
        if (!enabled) {
            return false;
        }

        try {
            Map<?, ?> response = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(Map.class);

            return response != null && "UP".equals(response.get("status"));
        } catch (Exception e) {
            log.debug("ML service health check failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
