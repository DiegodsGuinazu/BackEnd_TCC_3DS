package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Entity.User.*;
import br.com.neurohelp.tcc_backend.Repository.*;
import br.com.neurohelp.tcc_backend.Service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class ConfirmacaoEmailTest {
    @Autowired MockMvc mvc;
    @Autowired ConfirmacaoEmailRepository codigos;
    @Autowired RecuperacaoSenhaRepository recuperacoes;
    @Autowired profissionalRepository profissionais;
    @Autowired responsavelRepository responsaveis;
    @Autowired AdminRepository admins;
    @Autowired PasswordEncoder encoder;
    @MockitoBean EmailRecuperacaoService emails;
    String email, ip;
    private final List<String> usados = new ArrayList<>();
    @BeforeEach void preparar() {
        when(emails.configurado()).thenReturn(true);
        email = "confirmar-" + UUID.randomUUID() + "@teste.invalid";
        usados.add(email); ip = UUID.randomUUID().toString();
    }
    @AfterEach void limpar() {
        codigos.deleteAll(); recuperacoes.deleteAll();
        for (String e : usados) {
            profissionais.findByEmailIgnoreCase(e).ifPresent(profissionais::delete);
            responsaveis.findByEmailIgnoreCase(e).ifPresent(responsaveis::delete);
            admins.findByEmailIgnoreCase(e).ifPresent(admins::delete);
        }
    }
    private ResultActions pedir(String e) throws Exception {
        return mvc.perform(post("/cadastro/email/solicitar").with(r -> { r.setRemoteAddr(ip); return r; })
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + e + "\"}"));
    }
    private String solicitar() throws Exception {
        pedir(email).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        var c = ArgumentCaptor.forClass(String.class);
        verify(emails, timeout(5000)).enviarConfirmacao(eq(email), c.capture());
        return c.getValue();
    }
    private ResultActions cadastrar(String perfil, String e, String codigo) throws Exception {
        return mvc.perform(post("/cadastro/" + perfil).contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Novo usuário","email":"%s","senha":"Senha@123","cpf":"52998224725","codigoEmail":%s}
                """.formatted(e, codigo == null ? "null" : "\"" + codigo + "\"")));
    }
    @ParameterizedTest @ValueSource(strings = {"profissional", "responsavel"})
    void novoCadastroExigeCodigoECriaContaSomenteAposConfirmacao(String perfil) throws Exception {
        cadastrar(perfil, email, null).andExpect(status().is(422)).andExpect(jsonPath("$.codigo").value("EMAIL_NAO_CONFIRMADO"));
        assertFalse(profissionais.findByEmailIgnoreCase(email).isPresent());
        assertFalse(responsaveis.findByEmailIgnoreCase(email).isPresent());
        String codigo = solicitar();
        assertTrue(codigo.matches("[0-9]{8}"));
        assertFalse(codigos.findAll().getFirst().getCodigoHash().contains(codigo));
        assertFalse(codigos.findAll().getFirst().getEmailHash().contains(email));
        cadastrar(perfil, " " + email.toUpperCase(Locale.ROOT) + " ", codigo).andExpect(status().isOk());
        assertTrue(codigos.findAll().getFirst().isUtilizado());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        cadastrar(perfil.equals("profissional") ? "responsavel" : "profissional", email, codigo).andExpect(status().isConflict());
        verify(emails, never()).enviar(anyString(), anyString());
    }
    @Test void tentativasInvalidasPersistemEAQuintaBloqueiaInclusiveCodigoCorreto() throws Exception {
        String correto = solicitar();
        String errado = correto.equals("00000000") ? "00000001" : "00000000";
        for (int i = 0; i < 5; i++) cadastrar("responsavel", email, errado).andExpect(status().is(422));
        assertEquals(5, codigos.findAll().getFirst().getTentativas());
        cadastrar("responsavel", email, correto).andExpect(status().is(422));
        assertFalse(responsaveis.findByEmailIgnoreCase(email).isPresent());
    }
    @Test void codigoNaoServeParaOutroEmailNemParaRecuperarSenha() throws Exception {
        String codigo = solicitar();
        cadastrar("responsavel", "outro-" + email, codigo).andExpect(status().is(422));
        mvc.perform(post("/auth/redefinir-senha").with(r -> { r.setRemoteAddr(ip); return r; })
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","codigo":"%s","novaSenha":"Outra@123"}
                        """.formatted(email, codigo))).andExpect(status().isBadRequest());
        cadastrar("responsavel", email, codigo).andExpect(status().isOk());
    }
    @Test void expiracaoEReenvioInvalidamCodigoAnteriorERespeitamIntervalo() throws Exception {
        String antigo = solicitar();
        pedir(email).andExpect(status().isOk());
        verify(emails, times(1)).enviarConfirmacao(eq(email), anyString());
        var registro = codigos.findAll().getFirst();
        registro.setExpiraEm(Instant.now().minusSeconds(1));
        registro.setEnviadoEm(Instant.now().minusSeconds(61)); codigos.saveAndFlush(registro);
        cadastrar("responsavel", email, antigo).andExpect(status().is(422));
        pedir(email).andExpect(status().isOk());
        var c = ArgumentCaptor.forClass(String.class);
        verify(emails, timeout(5000).times(2)).enviarConfirmacao(eq(email), c.capture());
        String novo = c.getAllValues().getLast();
        if (!novo.equals(antigo)) cadastrar("responsavel", email, antigo).andExpect(status().is(422));
        cadastrar("responsavel", email, novo).andExpect(status().isOk());
    }
    @Test void senhaInvalidaNaoConsomeCodigo() throws Exception {
        String codigo = solicitar();
        mvc.perform(post("/cadastro/responsavel").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Teste","email":"%s","senha":"fraca","codigoEmail":"%s"}
                """.formatted(email, codigo))).andExpect(status().isBadRequest());
        assertFalse(codigos.findAll().getFirst().isUtilizado());
        cadastrar("responsavel", email, codigo).andExpect(status().isOk());
    }
    @ParameterizedTest @ValueSource(strings = {"profissional", "responsavel", "admin"})
    void contasAnterioresContinuamEntrandoSemConfirmarERecebemRespostaGenerica(String perfil) throws Exception {
        String senha = encoder.encode("Senha@123");
        if (perfil.equals("admin")) {
            var u = new UserAdmin(); u.setNome("Anterior"); u.setEmail(email); u.setSenha(senha); admins.saveAndFlush(u);
        } else if (perfil.equals("profissional")) {
            var u = new UserProf(); u.setNome("Anterior"); u.setEmail(email); u.setSenha(senha); u.setCpf("52998224725"); profissionais.saveAndFlush(u);
        } else {
            var u = new UserResp(); u.setNome("Anterior"); u.setEmail(email); u.setSenha(senha); responsaveis.saveAndFlush(u);
        }
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isOk());
        String resposta = pedir(email).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String outro = pedir("livre-" + email).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(resposta, outro);
        verify(emails, never()).enviarConfirmacao(eq(email), anyString());
    }
    @Test void limitaSolicitacoesERecusaEmailInvalidoSemCriarConta() throws Exception {
        pedir("invalido").andExpect(status().isBadRequest());
        for (int i = 0; i < 3; i++) pedir(email).andExpect(status().isOk());
        pedir(email).andExpect(status().isTooManyRequests());
        when(emails.configurado()).thenReturn(false);
        pedir("outro-" + email).andExpect(status().isServiceUnavailable());
    }
    @Test void consumoConcorrenteCriaSomenteUmaConta() throws Exception {
        String codigo = solicitar(); var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> acao = () -> { inicio.await(); return cadastrar("responsavel", email, codigo).andReturn().getResponse().getStatus(); };
            var a = executor.submit(acao); var b = executor.submit(acao); inicio.countDown();
            var resultados = List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(1, resultados.stream().filter(s -> s == 200).count());
            assertTrue(resultados.stream().allMatch(s -> s == 200 || s == 409 || s == 422));
        }
    }
}
