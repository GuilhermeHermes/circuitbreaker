package com.guilhermehermes.circuitbreaker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PaiService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String SERVICE_DEPENDENTE_URL = "http://localhost:8080/api/dependente";

    // Configura o Circuit Breaker com o nome "servicoPai"
    public String chamarDependente() {
        // Faz uma requisição HTTP para o Serviço Dependente
        ResponseEntity<String> response = restTemplate.getForEntity(SERVICE_DEPENDENTE_URL, String.class);
        return response.getBody();
    }

    // Método de fallback chamado quando o Circuit Breaker é aberto
    public String fallbackMethod(Exception ex) {
        return "Serviço Dependente está fora do ar. Por favor, tente mais tarde.";
    }
}
