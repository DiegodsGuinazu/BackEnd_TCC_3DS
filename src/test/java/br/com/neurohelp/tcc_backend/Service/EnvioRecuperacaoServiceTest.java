package br.com.neurohelp.tcc_backend.Service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.mockito.Mockito.*;

class EnvioRecuperacaoServiceTest {
    @Test void enviaSomenteAposCommit() {
        var emails = mock(EmailRecuperacaoService.class);
        when(emails.enviar(anyString(), anyString())).thenReturn(true);
        var envio = new EnvioRecuperacaoService(emails, Runnable::run);
        TransactionSynchronizationManager.initSynchronization();
        try {
            envio.agendar("teste@teste.invalid", "01234567");
            verifyNoInteractions(emails);
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
            verify(emails).enviar("teste@teste.invalid", "01234567");
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test void rollbackNaoEnvia() {
        var emails = mock(EmailRecuperacaoService.class);
        var envio = new EnvioRecuperacaoService(emails, Runnable::run);
        TransactionSynchronizationManager.initSynchronization();
        try {
            envio.agendar("teste@teste.invalid", "01234567");
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(1));
            verifyNoInteractions(emails);
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
}
