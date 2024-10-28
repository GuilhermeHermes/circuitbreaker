package com.guilhermehermes.circuitbreaker;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SimulacaoRequisicoes implements CommandLineRunner {

    private final RestTemplate restTemplate;
    private static final String SERVICO_PAI_URL = "http://localhost:8080/api/pai";
    private static final String ESTADO_SERVICO_PAI_URL = "http://localhost:8080/api/estado";

    public SimulacaoRequisicoes(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Iniciando simulação de requisições...");

        // Simulando múltiplas requisições para o Serviço Pai
        for (int i = 0; i < 10; i++) {

            try {
                String resposta = restTemplate.getForObject(SERVICO_PAI_URL, String.class);
                System.out.println("Resposta do Serviço Pai: " + resposta);
                String estado = restTemplate.getForObject(ESTADO_SERVICO_PAI_URL, String.class);
                System.out.println("Estado do Circuit Breaker: " + estado);

            } catch (Exception ex) {
                System.out.println("Erro na requisição: " + ex.getMessage());
            }

            // Aguarda 1 segundo entre as requisições para observar o comportamento do Circuit Breaker
            Thread.sleep(1000);
        }

        System.out.println("Simulação finalizada.");
    }
}

