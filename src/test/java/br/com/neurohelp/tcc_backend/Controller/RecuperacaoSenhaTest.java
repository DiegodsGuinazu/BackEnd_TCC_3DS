package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Entity.User.*;
import br.com.neurohelp.tcc_backend.Repository.*;
import br.com.neurohelp.tcc_backend.Security.TokenService;
import br.com.neurohelp.tcc_backend.Service.EmailRecuperacaoService;
import br.com.neurohelp.tcc_backend.Service.EnvioRecuperacaoService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class RecuperacaoSenhaTest {
    @Autowired MockMvc mvc;
    @Autowired responsavelRepository responsaveis;
    @Autowired profissionalRepository profissionais;
    @Autowired RecuperacaoSenhaRepository recuperacoes;
    @Autowired TokenService tokens;
    @Autowired PasswordEncoder encoder;
    @MockitoBean EmailRecuperacaoService emails;
    @MockitoBean EnvioRecuperacaoService envio;
    UserResp resp;
    @BeforeEach void preparar() {
        when(emails.configurado()).thenReturn(true);

        resp = new UserResp(); resp.setNome("Teste"); resp.setEmail("resp@teste.invalid");
        resp.setSenha(encoder.encode("Anterior@123")); responsaveis.saveAndFlush(resp);
    }
    String solicitar(String email) throws Exception {
        mvc.perform(post("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\"}")).andExpect(status().isAccepted());
        var codigo = ArgumentCaptor.forClass(String.class);
        verify(envio).agendar(eq(email.toLowerCase(java.util.Locale.ROOT)), codigo.capture());
        assertTrue(codigo.getValue().matches("[0-9]{8}"));
        assertNotEquals(codigo.getValue(), recuperacoes.findById(email.toLowerCase(java.util.Locale.ROOT)).orElseThrow().getCodigoHash());
        return codigo.getValue();
    }
    org.springframework.test.web.servlet.ResultActions redefinir(String email, String codigo) throws Exception {
        return mvc.perform(post("/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\""+email+"\",\"codigo\":\""+codigo+"\",\"novaSenha\":\"NovaSenha@123\"}"));
    }
    @Test void trocaSenhaUmaVezERevogaSessaoAnterior() throws Exception {
        String jwt = tokens.gerarToken(resp);
        String codigo = solicitar("RESP@TESTE.INVALID");
        redefinir(resp.getEmail(), codigo).andExpect(status().isOk());
        assertTrue(encoder.matches("NovaSenha@123", responsaveis.findByEmail(resp.getEmail()).orElseThrow().getSenha()));
        redefinir(resp.getEmail(), codigo).andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"resp@teste.invalid\",\"senha\":\"Anterior@123\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"resp@teste.invalid\",\"senha\":\"NovaSenha@123\"}")).andExpect(status().isOk());
    }
    @Test void perfilSempreEnviaParaEmailDoBanco() throws Exception {
        mvc.perform(post("/api/conta/senha/solicitar").header("Authorization", "Bearer " + tokens.gerarToken(resp))
            .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"outro@teste.invalid\"}"))
            .andExpect(status().isAccepted());
        verify(envio).agendar(eq(resp.getEmail()), anyString());
        mvc.perform(post("/api/conta/senha/solicitar")).andExpect(status().isUnauthorized());
    }
    @Test void profissionalTambemRecuperaSenha() throws Exception {
        var prof = new UserProf(); prof.setNome("Prof"); prof.setEmail("prof@teste.invalid");
        prof.setCpf("52998224725"); prof.setSenha(encoder.encode("Anterior@123")); profissionais.saveAndFlush(prof);
        String jwt = tokens.gerarToken(prof);
        redefinir(prof.getEmail(), solicitar(prof.getEmail())).andExpect(status().isOk());
        assertTrue(encoder.matches("NovaSenha@123", profissionais.findByEmail(prof.getEmail()).orElseThrow().getSenha()));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }
    @Test void codigoNaoPodeSerUsadoParaOutroEmail() throws Exception {
        String codigo = solicitar(resp.getEmail());
        redefinir("outra@teste.invalid", codigo).andExpect(status().isBadRequest());
        assertTrue(encoder.matches("Anterior@123", resp.getSenha()));
    }
    @Test void expiraENaoPermiteRenovarTentativasComReenvio() throws Exception {
        String codigo = solicitar(resp.getEmail());
        String errado = codigo.equals("00000000") ? "11111111" : "00000000";
        for (int i = 0; i < 5; i++) redefinir(resp.getEmail(), errado).andExpect(status().isBadRequest());
        mvc.perform(post("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"resp@teste.invalid\"}")).andExpect(status().isAccepted());
        verify(envio, times(1)).agendar(anyString(), anyString());
        redefinir(resp.getEmail(), codigo).andExpect(status().isBadRequest());
        assertTrue(encoder.matches("Anterior@123", resp.getSenha()));
    }
    @Test void codigoExpiradoFalha() throws Exception {
        String codigo = solicitar(resp.getEmail());
        recuperacoes.findById(resp.getEmail()).orElseThrow().setExpiraEm(Instant.now().minusSeconds(1));
        redefinir(resp.getEmail(), codigo).andExpect(status().isBadRequest());
    }
    @Test void novoCodigoAposPrazoInvalidaAnterior() throws Exception {
        String anterior = solicitar(resp.getEmail());
        var registro = recuperacoes.findById(resp.getEmail()).orElseThrow();
        String hashAnterior = registro.getCodigoHash();
        registro.setSolicitadoEm(Instant.now().minusSeconds(901)); registro.setExpiraEm(Instant.now().minusSeconds(1));
        mvc.perform(post("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"resp@teste.invalid\"}")).andExpect(status().isAccepted());
        var captor = ArgumentCaptor.forClass(String.class);
        verify(envio, times(2)).agendar(eq(resp.getEmail()), captor.capture());
        String atual = captor.getAllValues().get(1);
        assertNotEquals(hashAnterior, registro.getCodigoHash());
        if (!atual.equals(anterior)) redefinir(resp.getEmail(), anterior).andExpect(status().isBadRequest());
        redefinir(resp.getEmail(), atual).andExpect(status().isOk());
        verify(envio, times(2)).agendar(anyString(), anyString());
    }
    @Test void contaInexistenteTemMesmoRetornoSemEmail() throws Exception {
        String conhecido = mvc.perform(post("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"resp@teste.invalid\"}")).andReturn().getResponse().getContentAsString();
        String desconhecido = mvc.perform(post("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"ausente@teste.invalid\"}")).andExpect(status().isAccepted())
            .andReturn().getResponse().getContentAsString();
        assertEquals(conhecido, desconhecido);
        verify(envio, times(1)).agendar(anyString(), anyString());
    }
    @Test void faltaConfiguracaoNaoEnviaENaoDerrubaLogin() throws Exception {
        when(emails.configurado()).thenReturn(false);
        mvc.perform(post("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"resp@teste.invalid\"}")).andExpect(status().isServiceUnavailable());
        verify(envio, never()).agendar(anyString(), anyString());
    }
    @Test void senhaFracaNaoConsomeCodigo() throws Exception {
        String codigo = solicitar(resp.getEmail());
        mvc.perform(post("/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"resp@teste.invalid\",\"codigo\":\""+codigo+"\",\"novaSenha\":\"fraca\"}"))
            .andExpect(status().isBadRequest());
        redefinir(resp.getEmail(), codigo).andExpect(status().isOk());
    }
    @ParameterizedTest @ValueSource(strings = {"11111111111", "52998224724", "00000000000", "12345678900"})
    void cpfInvalidoBloqueadoNosDoisCadastros(String cpf) throws Exception {
        for (String perfil : new String[]{"responsavel", "profissional"})
            mvc.perform(post("/cadastro/"+perfil).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Teste\",\"email\":\"nova@teste.invalid\",\"senha\":\"Senha@123\",\"cpf\":\""+cpf+"\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void apiCpfPublicaDiferenciaDigitosDeConsultaCadastral() throws Exception {
        mvc.perform(post("/api/validacoes/cpf").contentType(MediaType.APPLICATION_JSON).content("{\"cpf\":\"529.982.247-25\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.valido").value(true))
            .andExpect(jsonPath("$.tipoValidacao").value("DIGITOS_VERIFICADORES"));
    }
}
