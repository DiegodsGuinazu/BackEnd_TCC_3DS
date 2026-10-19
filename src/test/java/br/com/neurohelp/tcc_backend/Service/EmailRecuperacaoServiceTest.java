package br.com.neurohelp.tcc_backend.Service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
}
