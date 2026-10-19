package br.com.neurohelp.tcc_backend.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RecuperacaoEmailListener {
    public record Envio(String email, String codigo) {
        @Override public String toString() { return "Envio[conteúdo protegido]"; }
    }
    private static final Logger log = LoggerFactory.getLogger(RecuperacaoEmailListener.class);
    private final EmailRecuperacaoService emails;
    private final ThreadPoolTaskExecutor executor;
    public RecuperacaoEmailListener(EmailRecuperacaoService emails, ThreadPoolTaskExecutor recuperacaoEmailExecutor) {
        this.emails = emails; this.executor = recuperacaoEmailExecutor;
    }
    @TransactionalEventListener
    public void enviarAposCommit(Envio envio) {
        try {
            executor.execute(() -> {
                try { emails.enviar(envio.email(), envio.codigo()); }
                catch (Exception e) { log.warn("Falha no envio de recuperação. Verifique a configuração e o provedor de e-mail."); }
            });
        } catch (org.springframework.core.task.TaskRejectedException e) {
            log.warn("Fila de recuperação ocupada. Solicite novo código após o intervalo.");
        }
    }
}
