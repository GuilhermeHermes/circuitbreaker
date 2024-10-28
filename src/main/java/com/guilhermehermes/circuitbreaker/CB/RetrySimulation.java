package com.guilhermehermes.circuitbreaker.CB;

import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RetrySimulation implements CommandLineRunner {

    private final RestTemplate restTemplate;
    private static final String SERVICO_PAI_URL = "http://localhost:8080/api/pai";
    private static final String ESTADO_SERVICO_PAI_URL = "http://localhost:8080/api/estado";
    private final RetryConfiguration retryConfiguration;
    private final RetryConfig retryConfig;

    public RetrySimulation(RestTemplate restTemplate, RetryConfiguration retryConfiguration, RetryConfig retryConfig) {
        this.restTemplate = restTemplate;
        this.retryConfiguration = retryConfiguration;
        this.retryConfig = retryConfig;
    }

    RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(2))
            .build();

    RetryRegistry registry = RetryRegistry.of(config);



    @Override
    public void run(String... args) throws Exception {
        System.out.println("Iniciando simulação de requisições...");

        AtomicInteger tentativaAtual = new AtomicInteger(0);

        // Simulando múltiplas requisições para o Serviço Pai
        for (int i = 0; i < 10; i++) {
            System.out.println("\n=== Requisição #" + (i + 1) + " ===");
            tentativaAtual.set(0);

            try {
                registry.retry("retry", config).executeCallable(() -> {
                    int attempt = tentativaAtual.incrementAndGet();
                    System.out.println(String.format(
                            ">>> Tentativa %d de %d",
                            attempt,
                            config.getMaxAttempts()
                    ));

                    try {
                        String resposta = restTemplate.getForObject(SERVICO_PAI_URL, String.class);
                        System.out.println("✅ Sucesso na tentativa " + attempt);
                        System.out.println("Resposta do Serviço Pai: " + resposta);

                        String estado = restTemplate.getForObject(ESTADO_SERVICO_PAI_URL, String.class);
                        System.out.println("Estado do Circuit Breaker: " + estado);

                        return null;
                    } catch (Exception e) {
                        System.out.println("❌ Falha na tentativa " + attempt + ": " + e.getMessage());
                        System.out.println("⏳ Aguardando ");
                        throw e;
                    }
                });

            } catch (Exception ex) {
                System.out.println("🚫 Todas as tentativas falharam para a requisição #" + (i + 1));
                System.out.println("Erro final: " + ex.getMessage());
            }

            // Aguarda 1 segundo entre as requisições
            System.out.println("\nAguardando 1 segundo antes da próxima requisição...");
            Thread.sleep(1000);
        }

        System.out.println("\nSimulação finalizada.");
    }
}

