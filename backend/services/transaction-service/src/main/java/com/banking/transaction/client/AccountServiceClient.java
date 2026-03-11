package com.banking.transaction.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
public class AccountServiceClient {

    private final WebClient webClient;

    public AccountServiceClient(WebClient.Builder builder,
                                @Value("${services.account-service.url}") String accountServiceUrl) {
        this.webClient = builder
                .baseUrl(accountServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void debit(String accountNumber, BigDecimal amount, String correlationId) {
        try {
            webClient.post()
                    .uri("/api/accounts/internal/debit")
                    .header("X-Correlation-ID", correlationId)
                    .bodyValue(Map.of("accountNumber", accountNumber, "amount", amount))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.debug("Debit successful for account [{}] amount [{}]", accountNumber, amount);
        } catch (WebClientResponseException ex) {
            log.error("Debit failed for account [{}]: HTTP {} - {}", accountNumber, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Debit failed for account " + accountNumber + ": " + ex.getResponseBodyAsString(), ex);
        }
    }

    public void credit(String accountNumber, BigDecimal amount, String correlationId) {
        try {
            webClient.post()
                    .uri("/api/accounts/internal/credit")
                    .header("X-Correlation-ID", correlationId)
                    .bodyValue(Map.of("accountNumber", accountNumber, "amount", amount))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.debug("Credit successful for account [{}] amount [{}]", accountNumber, amount);
        } catch (WebClientResponseException ex) {
            log.error("Credit failed for account [{}]: HTTP {} - {}", accountNumber, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Credit failed for account " + accountNumber + ": " + ex.getResponseBodyAsString(), ex);
        }
    }
}
