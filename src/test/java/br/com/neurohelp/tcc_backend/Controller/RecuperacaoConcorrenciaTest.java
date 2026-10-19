package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Repository.*;
import br.com.neurohelp.tcc_backend.Service.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest @AutoConfigureMockMvc
class RecuperacaoConcorrenciaTest {
    @Autowired RecuperacaoSenhaService service;
    @Autowired responsavelRepository responsaveis;
    @Autowired RecuperacaoSenhaRepository recuperacoes;
    @Autowired PasswordEncoder encoder;
    @MockitoBean EmailRecuperacaoService emails;
    @MockitoBean EnvioRecuperacaoService envio;
    @Test void pedidosSimultaneosEnviamUmaVezEConfirmamUmaVez() throws Exception {
        when(emails.configurado()).thenReturn(true);
        var usuario = new UserResp(); usuario.setNome("Concorrência");
        usuario.setEmail("concorrencia-"+UUID.randomUUID()+"@teste.invalid"); usuario.setSenha(encoder.encode("Anterior@123"));
        usuario = responsaveis.saveAndFlush(usuario);
        String email = usuario.getEmail();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var inicio = new CyclicBarrier(2);
            Callable<Boolean> solicitar = () -> { inicio.await(10, TimeUnit.SECONDS); service.solicitar(email); return true; };
            var a = pool.submit(solicitar); var b = pool.submit(solicitar);
            assertTrue(a.get(20, TimeUnit.SECONDS)); assertTrue(b.get(20, TimeUnit.SECONDS));
            var codigo = ArgumentCaptor.forClass(String.class);
            verify(envio, times(1)).agendar(eq(email), codigo.capture());
            var confirmarInicio = new CyclicBarrier(2);
            Callable<Boolean> confirmar = () -> { confirmarInicio.await(10, TimeUnit.SECONDS); return service.redefinir(email, codigo.getValue(), "NovaSenha@123"); };
            var c = pool.submit(confirmar); var d = pool.submit(confirmar);
            assertNotEquals(c.get(20, TimeUnit.SECONDS), d.get(20, TimeUnit.SECONDS));
            assertTrue(encoder.matches("NovaSenha@123", responsaveis.findByEmail(email).orElseThrow().getSenha()));
        } finally {
            pool.shutdownNow(); recuperacoes.deleteById(email); responsaveis.deleteById((long)usuario.getId());
        }
    }
}
