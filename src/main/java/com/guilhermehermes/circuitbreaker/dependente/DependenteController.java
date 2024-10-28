package com.guilhermehermes.circuitbreaker.dependente;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@RestController
public class DependenteController {
    private AtomicInteger falhaCount = new AtomicInteger(0);
    private static final int FALHAS_ANTES_RECUPERACAO = 2;
    private final RateLimiter rateLimiter;

    public DependenteController() {
        // Configuração do Rate Limiter
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(1)                     // Número de chamadas permitidas
                .limitRefreshPeriod(Duration.ofSeconds(2)) // Período de refresh
                .timeoutDuration(Duration.ofMillis(500))   // Timeout para aguardar permissão
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        this.rateLimiter = registry.rateLimiter("dependenteService");

        // Configurando listeners de eventos
        rateLimiter.getEventPublisher()
                .onSuccess(event -> System.out.println("Rate limit: Chamada permitida"))
                .onFailure(event -> System.out.println("Rate limit: Chamada rejeitada"));}

    @GetMapping("/api/dependente")
    public ResponseEntity<ServiceResponse> servicoDependente() {
        Supplier<ResponseEntity<ServiceResponse>> decoratedSupplier = RateLimiter
                .decorateSupplier(rateLimiter, () -> {
                    // Lógica original do serviço
                    int currentCount = falhaCount.get();

                    if (currentCount == 0) {
                        falhaCount.incrementAndGet();
                        return ResponseEntity.ok(new ServiceResponse(
                                "Resposta do Serviço Dependente",
                                true,
                                rateLimiter.getMetrics().getAvailablePermissions()
                        ));
                    }

                    if (currentCount < FALHAS_ANTES_RECUPERACAO) {
                        falhaCount.incrementAndGet();
                        throw new RuntimeException("Erro simulado no serviço dependente");
                    }

                    falhaCount.set(0);
                    return ResponseEntity.ok(new ServiceResponse(
                            "Resposta do Serviço Dependente - Recuperado",
                            true,
                            rateLimiter.getMetrics().getAvailablePermissions()
                    ));
                });

        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            if (e instanceof io.github.resilience4j.ratelimiter.RequestNotPermitted) {
                return ResponseEntity
                        .status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new ServiceResponse(
                                "Limite de requisições excedido. Tente novamente em alguns instantes.",
                                false,
                                rateLimiter.getMetrics().getAvailablePermissions()
                        ));
            }

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ServiceResponse(
                            "Erro no serviço: " + e.getMessage(),
                            false,
                            rateLimiter.getMetrics().getAvailablePermissions()
                    ));
        }
    }
}