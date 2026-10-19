package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Entity.User.*;
import br.com.neurohelp.tcc_backend.Repository.*;
import br.com.neurohelp.tcc_backend.Security.TokenService;
import br.com.neurohelp.tcc_backend.Service.*;
import br.com.neurohelp.tcc_backend.DTO.RecuperacaoSenhaDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
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
class RecuperacaoSenhaTest {
    @Autowired MockMvc mvc;
    @Autowired profissionalRepository profissionais;
    @Autowired responsavelRepository responsaveis;
    @Autowired RecuperacaoSenhaRepository codigos;
    @Autowired TokenService tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired RecuperacaoSenhaService service;
    @MockitoBean EmailRecuperacaoService emails;
    private final List<UserResp> criados = new ArrayList<>();
    private final List<UserProf> profs = new ArrayList<>();
    private String ip;
    @BeforeEach void preparar() { when(emails.configurado()).thenReturn(true); ip = UUID.randomUUID().toString(); }
    @AfterEach void limpar() { codigos.deleteAll(); responsaveis.deleteAll(criados); profissionais.deleteAll(profs); }
    private UserResp conta() {
        UserResp u = new UserResp(); u.setEmail("rec-" + UUID.randomUUID() + "@teste.invalid");
        u.setNome("Teste"); u.setSenha(encoder.encode("Anterior@123"));
        u = responsaveis.saveAndFlush(u); criados.add(u); return u;
    }
    private String solicitar(UsuarioAutenticavel u) throws Exception {
        mvc.perform(post("/auth/esqueci-senha").with(r -> { r.setRemoteAddr(ip); return r; })
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + u.getEmail() + "\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(emails, timeout(5000)).enviar(eq(u.getEmail()), captor.capture());
        return captor.getValue();
    }
    private org.springframework.test.web.servlet.ResultActions confirmar(String email, String codigo, String senha) throws Exception {
        return mvc.perform(post("/auth/redefinir-senha").with(r -> { r.setRemoteAddr(ip); return r; })
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","codigo":"%s","novaSenha":"%s"}
                        """.formatted(email, codigo, senha)));
    }
    @Test void responsavelRecebeCodigoAlteraSenhaRevogaTokensEImpedeReutilizacao() throws Exception {
        var u = conta(); String antigo = tokens.gerarToken(u);
        String codigo = solicitar(u);
        assertTrue(codigo.matches("[0-9]{8}"));
        assertFalse(codigos.findAll().getFirst().getCodigoHash().contains(codigo));
        confirmar(u.getEmail(), codigo, "NovaSenha@123").andExpect(status().isOk());
        var atualizado = responsaveis.findByEmail(u.getEmail()).orElseThrow();
        assertTrue(encoder.matches("NovaSenha@123", atualizado.getSenha()));
        assertEquals(1, atualizado.getVersaoCredenciais());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + antigo)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokens.gerarToken(atualizado))).andExpect(status().isOk());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + u.getEmail() + "\",\"senha\":\"Anterior@123\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + u.getEmail() + "\",\"senha\":\"NovaSenha@123\"}")).andExpect(status().isOk());
        confirmar(u.getEmail(), codigo, "OutraSenha@123").andExpect(status().isBadRequest());
    }
    @Test void profissionalRecebeNoEmailDoBancoMesmoComDestinatarioForjado() throws Exception {
        UserProf u = new UserProf(); u.setEmail("prof-" + UUID.randomUUID() + "@teste.invalid");
        u.setCpf("52998224725"); u.setSenha(encoder.encode("Anterior@123"));
        u = profissionais.saveAndFlush(u); profs.add(u);
        mvc.perform(post("/api/conta/recuperacao-senha").with(r -> { r.setRemoteAddr(ip); return r; })
                .header("Authorization", "Bearer " + tokens.gerarToken(u)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"intruso@teste.invalid\"}")).andExpect(status().isOk());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(emails, timeout(5000)).enviar(eq(u.getEmail()), captor.capture());
        verify(emails, never()).enviar(eq("intruso@teste.invalid"), anyString());
        confirmar(u.getEmail(), captor.getValue(), "NovaSenha@123").andExpect(status().isOk());
        assertTrue(encoder.matches("NovaSenha@123", profissionais.findByEmail(u.getEmail()).orElseThrow().getSenha()));
        mvc.perform(post("/api/conta/recuperacao-senha")).andExpect(status().isUnauthorized());
    }
    @Test void emailDesconhecidoTemMesmaRespostaESemEnvio() throws Exception {
        var u = conta();
        var conhecido = mvc.perform(post("/auth/esqueci-senha").with(r -> { r.setRemoteAddr(ip); return r; })
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + u.getEmail() + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/auth/esqueci-senha").with(r -> { r.setRemoteAddr(ip); return r; })
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ausente@teste.invalid\"}"))
                .andExpect(status().isOk()).andExpect(content().string(conhecido));
        verify(emails, never()).enviar(eq("ausente@teste.invalid"), anyString());
    }
    @Test void cincoErrosBloqueiamCodigoMesmoDepoisDeNovaTransacao() throws Exception {
        var u = conta(); String codigo = solicitar(u); String errado = codigo.equals("00000000") ? "11111111" : "00000000";
        for (int i = 0; i < 5; i++) confirmar(u.getEmail(), errado, "NovaSenha@123").andExpect(status().isBadRequest());
        assertEquals(5, codigos.findAll().getFirst().getTentativas());
        confirmar(u.getEmail(), codigo, "NovaSenha@123").andExpect(status().isBadRequest());
        assertTrue(encoder.matches("Anterior@123", responsaveis.findByEmail(u.getEmail()).orElseThrow().getSenha()));
    }
    @Test void expiracaoSenhaFracaEEmailDiferenteNaoAlteramSenha() throws Exception {
        var u = conta(); String codigo = solicitar(u);
        confirmar(u.getEmail(), codigo, "fraca").andExpect(status().isBadRequest());
        confirmar("outro@teste.invalid", codigo, "NovaSenha@123").andExpect(status().isBadRequest());
        var registro = codigos.findAll().getFirst(); registro.setExpiraEm(Instant.now().minusSeconds(1)); codigos.saveAndFlush(registro);
        confirmar(u.getEmail(), codigo, "NovaSenha@123").andExpect(status().isBadRequest());
    }
    @Test void reenvioRespeitaIntervaloEInvalidaCodigoAnterior() throws Exception {
        var u = conta(); String antigo = solicitar(u);
        service.solicitar(u.getEmail(), ip);
        verify(emails, times(1)).enviar(eq(u.getEmail()), anyString());
        var registro = codigos.findAll().getFirst(); registro.setEnviadoEm(Instant.now().minusSeconds(61)); codigos.saveAndFlush(registro);
        service.solicitar(u.getEmail(), ip);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(emails, timeout(5000).times(2)).enviar(eq(u.getEmail()), captor.capture());
        String novo = captor.getAllValues().getLast();
        if (!antigo.equals(novo)) confirmar(u.getEmail(), antigo, "NovaSenha@123").andExpect(status().isBadRequest());
        confirmar(u.getEmail(), novo, "NovaSenha@123").andExpect(status().isOk());
    }
    @Test void consumoConcorrenteTemSomenteUmVencedor() throws Exception {
        var u = conta(); String codigo = solicitar(u);
        var dto = new RecuperacaoSenhaDTO.Redefinir(u.getEmail(), codigo, "NovaSenha@123");
        CountDownLatch iniciar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> acao = () -> { iniciar.await(); try { service.redefinir(dto, UUID.randomUUID().toString()); return true; }
                catch (org.springframework.web.server.ResponseStatusException e) { assertEquals(400, e.getStatusCode().value()); return false; } };
            var a = executor.submit(acao); var b = executor.submit(acao); iniciar.countDown();
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        }
    }
    @Test void provedorAusenteRetorna503EExcessoRetorna429() throws Exception {
        when(emails.configurado()).thenReturn(false);
        mvc.perform(post("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@teste.invalid\"}"))
                .andExpect(status().isServiceUnavailable());
        when(emails.configurado()).thenReturn(true);
        String email = "limite-" + UUID.randomUUID() + "@teste.invalid";
        for (int i = 0; i < 3; i++) service.solicitar(email, ip);
        mvc.perform(post("/auth/esqueci-senha").with(r -> { r.setRemoteAddr(ip); return r; }).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}")).andExpect(status().isTooManyRequests());
    }
    @Test void erroDeEnvioNaoExpoeContaNemSegredos() throws Exception {
        doThrow(new IllegalStateException("segredo de teste")).when(emails).enviar(anyString(), anyString());
        solicitar(conta());
    }
    @Test void apiCpfExplicaLimiteEBackendRejeitaCpfInvalido() throws Exception {
        mvc.perform(post("/validacoes/cpf").contentType(MediaType.APPLICATION_JSON).content("{\"cpf\":\"529.982.247-25\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valido").value(true)).andExpect(jsonPath("$.cadastroConsultado").value(false));
        mvc.perform(post("/validacoes/cpf").contentType(MediaType.APPLICATION_JSON).content("{\"cpf\":\"11111111111\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valido").value(false));
        for (String perfil : List.of("responsavel", "profissional")) {
            mvc.perform(post("/cadastro/" + perfil).contentType(MediaType.APPLICATION_JSON).content("""
                    {"nome":"Teste","email":"cpf@teste.invalid","senha":"Senha@123","cpf":"12345678901"}
                    """ )).andExpect(status().isBadRequest()).andExpect(content().string("CPF inválido. Confira o número informado."));
        }
    }
}
