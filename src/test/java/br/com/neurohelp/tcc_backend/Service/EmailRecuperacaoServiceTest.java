package br.com.neurohelp.tcc_backend.Service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class EmailRecuperacaoServiceTest {
    @Test void selecionaGmailSemDependerDeCredenciaisResend() {
        var gmail = mock(GmailRecuperacaoService.class); when(gmail.configurado()).thenReturn(true);
        var service = new EmailRecuperacaoService("", "", "gmail", gmail);
        assertTrue(service.configurado());
        service.enviar("pessoa@teste.invalid", "01234567");
        verify(gmail).enviar("pessoa@teste.invalid", "01234567");
    }
    @Test void gmailIncompletoNaoCaiSilenciosamenteNaResend() {
        var gmail = mock(GmailRecuperacaoService.class);
        var service = new EmailRecuperacaoService("resend-teste", "remetente@teste.invalid", "gmail", gmail);
        assertFalse(service.configurado());
        assertThrows(IllegalStateException.class, () -> service.enviar("pessoa@teste.invalid", "01234567"));
        verify(gmail, never()).enviar(anyString(), anyString());
    }
    @Test void configuracaoResendAnteriorPermaneceAceita() {
        var gmail = mock(GmailRecuperacaoService.class);
        assertTrue(new EmailRecuperacaoService("resend-teste", "remetente@teste.invalid", "resend", gmail).configurado());
        verifyNoInteractions(gmail);
    }
    @Test void provedorDesconhecidoFicaIndisponivel() {
        var service = new EmailRecuperacaoService("resend-teste", "remetente@teste.invalid", "desconhecido", mock(GmailRecuperacaoService.class));
        assertFalse(service.configurado());
    }
    @Test void logMostraProvedorMasNuncaValoresDaConfiguracao(CapturedOutput output) {
        var gmail = mock(GmailRecuperacaoService.class);
        when(gmail.configurado()).thenReturn(true);
        new EmailRecuperacaoService("chave-privada", "remetente@teste.invalid", "gmail", gmail);
        new EmailRecuperacaoService("chave-privada", "remetente@teste.invalid", "valor-privado\nlog-forjado", gmail);
        assertTrue(output.getAll().contains("provedor=gmail, configurado=true"));
        assertTrue(output.getAll().contains("provedor=desconhecido, configurado=false"));
        for (String privado : new String[]{"chave-privada", "remetente@teste.invalid", "valor-privado", "log-forjado"})
            assertFalse(output.getAll().contains(privado));
    }
}
