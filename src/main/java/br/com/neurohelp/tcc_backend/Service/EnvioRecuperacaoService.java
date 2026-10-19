package br.com.neurohelp.tcc_backend.Service;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.concurrent.*;

/** Entrega fora da resposta HTTP e apenas após confirmar a transação. */
@Service
public class EnvioRecuperacaoService {
    private final EmailRecuperacaoService emails;
    private final Executor executor;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EnvioRecuperacaoService.class);
    @Autowired
    public EnvioRecuperacaoService(EmailRecuperacaoService emails) {
        this(emails, new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), runnable -> {
            Thread thread = new Thread(runnable, "email-recuperacao"); thread.setDaemon(true); return thread;
        }, new ThreadPoolExecutor.AbortPolicy()));
    }
    EnvioRecuperacaoService(EmailRecuperacaoService emails, Executor executor) {
        this.emails = emails; this.executor = executor;
    }
    public void agendar(String destinatario, String codigo) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try {
                    executor.execute(() -> {
                        if (!emails.enviar(destinatario, codigo)) log.warn("Falha na entrega de e-mail de recuperação. Verifique a configuração do provedor.");
                    });
                } catch (RejectedExecutionException e) {
                    log.warn("Fila de recuperação de senha indisponível. Tente novamente após o prazo do código.");
                }
            }
        });
    }
    @PreDestroy public void fechar() {
        if (executor instanceof ExecutorService pool) pool.shutdown();
    }
}
