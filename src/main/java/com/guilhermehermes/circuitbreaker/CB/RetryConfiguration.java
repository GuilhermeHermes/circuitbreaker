package com.guilhermehermes.circuitbreaker.CB;

import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Configuration
public class RetryConfiguration {

    @Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)                     // Número máximo de tentativas
                .waitDuration(Duration.ofSeconds(2)) // Tempo de espera entre tentativas
                .retryOnResult(response -> {
                    // Adicione aqui condições para retry baseado na resposta
                    return response == null;
                })
                .retryOnException(e -> {
                    // Você pode especificar quais exceções devem trigger o retry
                    return e instanceof RestClientException;
                })
                .failAfterMaxAttempts(true)         // Lança exceção após máximo de tentativas
                .build();
    }
}
