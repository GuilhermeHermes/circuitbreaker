package com.guilhermehermes.circuitbreaker.dependente;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(1)                     // Número de chamadas permitidas
            .limitRefreshPeriod(Duration.ofSeconds(2)) // Período de refresh
            .timeoutDuration(Duration.ofMillis(500))   // Timeout para aguardar permissão
            .build();

    @Bean
    public RateLimiter dependenteServiceRateLimiter() {


        return RateLimiterRegistry.of(config)
                .rateLimiter("dependenteService");
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.of(config);
    }
}