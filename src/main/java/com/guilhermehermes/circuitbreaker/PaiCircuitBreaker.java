package com.guilhermehermes.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
public class PaiCircuitBreaker {
private final CircuitBreaker circuitBreaker;
private final CircuitBreakerRegistry circuitBreakerRegistry;
private final PaiService paiService;

public PaiCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry, PaiService paiService) {
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.paiService = paiService;

    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(5))  // Reduzido para facilitar testes
            .slidingWindowSize(4)
            .minimumNumberOfCalls(2)
            .permittedNumberOfCallsInHalfOpenState(2)  // Permite 2 chamadas no estado HALF_OPEN
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .build();

    this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("chamarDependente", config);
    configureEventListeners();
}


@GetMapping("/pai")
public ResponseEntity<String> executeChamarDependente() {
    try {
        String resultado = circuitBreaker.executeSupplier(() -> {
            return paiService.chamarDependente();
        });

        // Adiciona o estado atual do circuit breaker na resposta
        CircuitBreaker.State estado = circuitBreaker.getState();
        CircuitBreaker.Metrics metricas = circuitBreaker.getMetrics();

        String resposta = String.format(
                "Estado: %s, Resposta: %s, Métricas: {sucessos: %d, falhas: %d, taxa_falhas: %.2f%%}",
                estado,
                resultado,
                metricas.getNumberOfSuccessfulCalls(),
                metricas.getNumberOfFailedCalls(),
                metricas.getFailureRate()
        );

        return ResponseEntity.ok(resposta);
    } catch (CallNotPermittedException e) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Circuito ABERTO - Serviço Dependente indisponível!");
    } catch (Exception e) {
        log.error("Erro ao chamar dependente", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro: " + e.getMessage());
    }
}

    @GetMapping("/estado")
    public Map<String, Object> getEstadoCircuito() {
        return Map.of(
                "estado", circuitBreaker.getState(),
                "metricas", Map.of(
                        "falhas", circuitBreaker.getMetrics().getNumberOfFailedCalls(),
                        "sucessos", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls(),
                        "total", circuitBreaker.getMetrics().getNumberOfBufferedCalls()
                ),
                "configuracao", Map.of(
                        "limiarFalhas", circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold(),
                        "tempoEmAberto", circuitBreaker.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState()
                )
        );
    }

    private void configureEventListeners() {
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    log.info("🔄 Mudança de Estado do Circuit Breaker: {} -> {}",
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState());
                })
                .onSuccess(event -> {
                    log.info("✅ Chamada ao Dependente bem-sucedida");
                })
                .onError(event -> {
                    log.error("❌ Erro na chamada ao Dependente: {}",
                            event.getThrowable().getMessage());
                })
                .onCallNotPermitted(event -> {
                    log.warn("⛔ Chamada ao Dependente bloqueada - circuito aberto");
                });
    }
}

