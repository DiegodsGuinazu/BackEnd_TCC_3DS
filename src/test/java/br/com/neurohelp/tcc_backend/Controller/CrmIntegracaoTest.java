package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Entity.User.*;
import br.com.neurohelp.tcc_backend.Repository.*;
import br.com.neurohelp.tcc_backend.Security.TokenService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class CrmIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired profissionalRepository profissionais;
    @Autowired responsavelRepository responsaveis;
    @Autowired AdminRepository admins;
    @Autowired TokenService tokens;
    @Autowired EntityManager em;
    @Value("${api.security.token.secret}") String secret;
    UserProf p; UserResp r; UserAdmin a;
    String bearer(UsuarioAutenticavel u) { return "Bearer " + tokens.gerarToken(u); }
    @BeforeEach void preparar() {
        p = new UserProf(); p.setNome("Profissional exemplo"); p.setEmail("crm@teste.invalid");
        p.setSenha("hash"); p.setCpf("11111111111"); p.setNumRegistro("CRM-SP 1234567"); profissionais.saveAndFlush(p);
        r = new UserResp(); r.setNome("Responsável"); r.setEmail("resp-crm@teste.invalid"); r.setSenha("hash"); responsaveis.saveAndFlush(r);
        a = new UserAdmin(); a.setNome("Equipe"); a.setEmail("admin-crm@teste.invalid"); a.setSenha("hash"); admins.saveAndFlush(a);
    }
    void solicitar() throws Exception {
        mvc.perform(post("/api/perfil/crm/solicitacao").header("Authorization", bearer(p)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_ANALISE"));
    }
    String decisao(boolean sim) { return "{\"revisao\":" + p.getCrmRevisao() + ",\"confirmado\":" + sim + "}"; }
    void revisar(boolean sim) throws Exception {
        mvc.perform(put("/api/admin/crm/solicitacoes/" + p.getId()).header("Authorization", bearer(a))
                .contentType(MediaType.APPLICATION_JSON).content(decisao(sim))).andExpect(status().isOk());
    }
    @Test void cadastroComCrmNaoExigeServicoPagoEMantemConfirmacaoEmail() throws Exception {
        ConfirmacaoEmailFixture.criar(em, secret, "novo-crm@teste.invalid");
        mvc.perform(post("/cadastro/profissional").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Nome exemplo","email":"novo-crm@teste.invalid","senha":"Senha@123","cpf":"52998224725",
                 "numRegistro":"CRM-SP 1234567","codigoEmail":"12345678","crmStatus":"VERIFICADO"}
                """)).andExpect(status().isOk());
        var novo = profissionais.findByEmail("novo-crm@teste.invalid").orElseThrow();
        assertNull(novo.getCrmStatus());
    }
    @Test void solicitacaoEIdempotenteEConferenciaAparecePublicamente() throws Exception {
        solicitar(); long revisao = p.getCrmRevisao(); solicitar(); assertEquals(revisao, p.getCrmRevisao());
        mvc.perform(get("/api/admin/crm/solicitacoes").header("Authorization", bearer(a)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].numRegistro").value(p.getNumRegistro()));
        revisar(true);
        mvc.perform(get("/api/profissionais")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].crmVerificacao.status").value("VERIFICADO"))
                .andExpect(jsonPath("$[0].crmVerificacao.conferidoEm").isNotEmpty())
                .andExpect(jsonPath("$[0].crmRevisorId").doesNotExist());
        mvc.perform(get("/api/profissionais/" + p.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/profissionais/" + p.getId()).header("Authorization", bearer(r)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.crmVerificacao.status").value("VERIFICADO"));
    }
    @Test void profissionalNaoPodeSeAprovarEVisitanteNaoPodeSolicitar() throws Exception {
        mvc.perform(post("/api/perfil/crm/solicitacao")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/perfil/crm/solicitacao").header("Authorization", bearer(r))).andExpect(status().isForbidden());
        solicitar();
        mvc.perform(get("/api/admin/crm/solicitacoes").header("Authorization", bearer(p))).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/crm/solicitacoes/" + p.getId()).header("Authorization", bearer(p))
                .contentType(MediaType.APPLICATION_JSON).content(decisao(true))).andExpect(status().isForbidden());
    }
    @Test void mudarNomeInvalidaSeloEReverterNomeNaoRestauraConferencia() throws Exception {
        solicitar(); revisar(true);
        for (String nome : new String[]{"Outro nome", "Profissional exemplo"})
            mvc.perform(put("/api/perfil").header("Authorization", bearer(p)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"" + nome + "\",\"crmStatus\":\"VERIFICADO\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.crmVerificacao.status").value("PENDENTE"));
    }
    @Test void editarBioPreservaConferenciaEAlterarCrmInvalida() throws Exception {
        solicitar(); revisar(true);
        mvc.perform(put("/api/perfil").header("Authorization", bearer(p)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bio\":\"Nova apresentação\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.crmVerificacao.status").value("VERIFICADO"));
        mvc.perform(put("/api/perfil").header("Authorization", bearer(p)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"numRegistro\":\"CRM-RJ 1234567\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.crmVerificacao.status").value("PENDENTE"));
    }
    @Test void revisaoAntigaNaoAprovaNovoPedidoENegativaNaoBloqueiaPerfil() throws Exception {
        solicitar(); String antigo = decisao(true); revisar(false);
        mvc.perform(get("/api/perfil").header("Authorization", bearer(p))).andExpect(status().isOk())
                .andExpect(jsonPath("$.crmVerificacao.status").value("NAO_CONFIRMADO"));
        solicitar();
        mvc.perform(put("/api/admin/crm/solicitacoes/" + p.getId()).header("Authorization", bearer(a))
                .contentType(MediaType.APPLICATION_JSON).content(antigo)).andExpect(status().isConflict());
    }
}
