package br.com.neurohelp.tcc_backend.Service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Limites adicionais por instância; cooldown e tentativas do código ficam no banco. */
@Component
public class RecuperacaoLimites {
    private record Janela(Instant fim, int total) {}
    private final Map<String, Janela> janelas = new HashMap<>();
    public synchronized void verificar(String chave, int maximo) {
        Instant agora = Instant.now();
        janelas.entrySet().removeIf(e -> !e.getValue().fim().isAfter(agora));
        Janela atual = janelas.get(chave);
        if (atual == null) {
            if (janelas.size() >= 10000) throw excesso();
            atual = new Janela(agora.plusSeconds(900), 0);
        }
        if (atual.total() >= maximo) throw excesso();
        janelas.put(chave, new Janela(atual.fim(), atual.total() + 1));
    }
    private ResponseStatusException excesso() {
        return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Aguarde 15 minutos e tente novamente.");
    }
}
