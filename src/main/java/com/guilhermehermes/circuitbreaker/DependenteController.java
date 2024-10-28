package com.guilhermehermes.circuitbreaker;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class DependenteController {
    private AtomicInteger falhaCount = new AtomicInteger(0);
    private static final int FALHAS_ANTES_RECUPERACAO = 2;

    @GetMapping("/api/dependente")
    public String servicoDependente() {
        int currentCount = falhaCount.get();

        // Primeiras chamadas são bem sucedidas
        if (currentCount == 0) {
            falhaCount.incrementAndGet();
            return "Resposta do Serviço Dependente";
        }

        // Próximas chamadas falham até atingir FALHAS_ANTES_RECUPERACAO
        if (currentCount < FALHAS_ANTES_RECUPERACAO) {
            falhaCount.incrementAndGet();
            throw new RuntimeException("Erro simulado no serviço dependente");
        }

        // Após FALHAS_ANTES_RECUPERACAO, serviço volta ao normal
        falhaCount.set(0);
        return "Resposta do Serviço Dependente - Recuperado";
    }
}
