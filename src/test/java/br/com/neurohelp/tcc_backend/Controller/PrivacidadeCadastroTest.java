package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Repository.*;
import br.com.neurohelp.tcc_backend.Entity.User.*;
import br.com.neurohelp.tcc_backend.Security.TokenService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class PrivacidadeCadastroTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired profissionalRepository profissionais;
    @Autowired responsavelRepository responsaveis;
    @Autowired TokenService tokens;
    @Value("${api.security.token.secret}") String secret;

    @Test void cadastroDoAppPersisteEnderecoEProfissaoSemExporSegredos() throws Exception {
        ConfirmacaoEmailFixture.criar(em, secret, "app@teste.invalid");
        mvc.perform(post("/cadastro/profissional").contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"Profissional Teste", "email":"app@teste.invalid", "senha":"Senha@123",
             "cpf":"52998224725", "telefone":"11999999999", "estado":"SP", "cidade":"São Paulo",
             "cep":"01001000", "bairro":"Sé", "logradouro":"Praça da Sé", "numero":"10",
             "profissao":"Psicologia", "numRegistro":"CRP 123", "codigoEmail":"12345678"}
            """)).andExpect(status().isOk());
        em.flush(); em.clear();
        var p = profissionais.findByEmailIgnoreCase("app@teste.invalid").orElseThrow();
        mvc.perform(get("/api/conta/privacidade").header("Authorization", "Bearer " + tokens.gerarToken(p)))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(jsonPath("$.formacao").value("Psicologia"))
            .andExpect(jsonPath("$.cep").value("01001000")).andExpect(jsonPath("$.bairro").value("Sé"))
            .andExpect(jsonPath("$.logradouro").value("Praça da Sé")).andExpect(jsonPath("$.numero").value("10"))
            .andExpect(jsonPath("$.telefone").value("11999999999"))
            .andExpect(jsonPath("$.senha").doesNotExist()).andExpect(jsonPath("$.versaoCredenciais").doesNotExist());
    }

    @Test void validacaoIndicaCamposSemDevolverValoresRejeitados() throws Exception {
        mvc.perform(post("/cadastro/profissional").contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"Teste", "email":"teste@teste.invalid", "senha":"segredo-invalido",
             "cpf":"52998224725", "telefone":"119999999999"}
            """)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.telefone").value("Telefone deve possuir 10 ou 11 dígitos."))
            .andExpect(jsonPath("$.campos.senha").exists())
            .andExpect(content().string(not(containsString("segredo-invalido"))))
            .andExpect(content().string(not(containsString("119999999999"))));
    }

    @Test void responsavelConsultaSomenteSuaConta() throws Exception {
        UserResp r = new UserResp(); r.setNome("Responsável"); r.setEmail("resp-priv@teste.invalid");
        r.setSenha("hash-nunca-retornado"); r.setTelefone("1133334444"); r.setCidade("São Paulo");
        responsaveis.saveAndFlush(r);
        mvc.perform(get("/api/conta/privacidade").header("Authorization", "Bearer " + tokens.gerarToken(r)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(r.getEmail()))
            .andExpect(jsonPath("$.telefone").value("1133334444"))
            .andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(get("/api/conta/privacidade")).andExpect(status().isUnauthorized());
    }
}
