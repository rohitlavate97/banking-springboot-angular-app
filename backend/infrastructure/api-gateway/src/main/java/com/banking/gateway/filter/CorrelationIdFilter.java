package com.banking.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders()
                .getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .build();

        log.debug("Request [{}] {} {}", finalCorrelationId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath().value());

        long startTime = System.currentTimeMillis();

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doOnSuccess(v -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Response [{}] {} {} → {} ({}ms)",
                            finalCorrelationId,
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getPath().value(),
                            exchange.getResponse().getStatusCode(),
                            duration);
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
