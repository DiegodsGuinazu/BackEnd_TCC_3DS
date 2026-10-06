package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.DTO.CadastroAdminDTO;
import br.com.neurohelp.tcc_backend.Entity.User.*;
import br.com.neurohelp.tcc_backend.Entity.Postagem.*;
import br.com.neurohelp.tcc_backend.Entity.Postagem.Anexo.Anexo;
import br.com.neurohelp.tcc_backend.Repository.*;
import br.com.neurohelp.tcc_backend.Security.TokenService;
import br.com.neurohelp.tcc_backend.Service.ConviteAdminService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class AdminIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired ConviteAdminService service;
    @Autowired ConviteAdminRepository convites;
    @Autowired AdminRepository admins;
    @Autowired responsavelRepository responsaveis;
    @Autowired profissionalRepository profissionais;
    @Autowired PostagemRepository postagens;
    @Autowired AnexoRepository anexos;
    @Autowired TokenService tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager em;

    String token(ConviteAdminService.ConviteGerado c) { return c.link().split("#token=")[1]; }
    String corpo(String token, String email) { return """
            {"nome":"Administrador teste","email":"%s","senha":"Senha@123","token":"%s"}
            """.formatted(email, token); }
    UserAdmin admin() {
        UserAdmin a = new UserAdmin(); a.setNome("Admin teste"); a.setEmail("admin@teste.invalid");
        a.setSenha(encoder.encode("Senha@123")); return admins.saveAndFlush(a);
    }
    UserResp responsavel() {
        UserResp r = new UserResp(); r.setNome("Responsável"); r.setEmail("comum@teste.invalid");
        r.setSenha(encoder.encode("Senha@123")); return responsaveis.saveAndFlush(r);
    }
    String bearer(UsuarioAutenticavel u) { return "Bearer " + tokens.gerarToken(u); }

    @Test void cadastroValidoHashLoginPerfilEReutilizacao() throws Exception {
        var c = service.gerarPrimeiro("convidado@teste.invalid"); String token = token(c);
        assertNotEquals(token, convites.findById(c.id()).orElseThrow().getTokenHash());
        assertEquals(64, convites.findById(c.id()).orElseThrow().getTokenHash().length());
        mvc.perform(post("/cadastro/admin/convite/validar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("convidado@teste.invalid"))
                .andExpect(jsonPath("$.token").doesNotExist()).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(post("/cadastro/admin").contentType(MediaType.APPLICATION_JSON).content(corpo(token, "convidado@teste.invalid")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.senha").doesNotExist());
        UserAdmin a = admins.findByEmailIgnoreCase("convidado@teste.invalid").orElseThrow();
        assertTrue(encoder.matches("Senha@123", a.getSenha()));
        assertNotNull(convites.findById(c.id()).orElseThrow().getUtilizadoEm());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"convidado@teste.invalid\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(a)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipoPerfil").value("ADMIN"))
                .andExpect(jsonPath("$.senha").doesNotExist()).andExpect(jsonPath("$.tokenHash").doesNotExist());
        mvc.perform(post("/cadastro/admin").contentType(MediaType.APPLICATION_JSON).content(corpo(token, a.getEmail())))
                .andExpect(status().isBadRequest());
    }

    @Test void rejeitaInvalidoExpiradoRevogadoSemCriarConta() throws Exception {
        List<String> invalidos = new ArrayList<>(List.of("A".repeat(43), "invalido"));
        var expirado = service.gerar("expirado@teste.invalid");
        convites.findById(expirado.id()).orElseThrow().setExpiraEm(Instant.now().minusSeconds(1));
        invalidos.add(token(expirado));
        var revogado = service.gerar("revogado@teste.invalid"); service.revogar(revogado.id()); invalidos.add(token(revogado));
        convites.flush();
        for (String token : invalidos) {
            mvc.perform(post("/cadastro/admin").contentType(MediaType.APPLICATION_JSON)
                    .content(corpo(token, "qualquer@teste.invalid"))).andExpect(status().isBadRequest());
        }
        assertEquals(0, admins.count());
    }

    @Test void emailDiferenteESenhaFracaNaoConsomemConvite() throws Exception {
        var c = service.gerar("autorizado@teste.invalid");
        mvc.perform(post("/cadastro/admin").contentType(MediaType.APPLICATION_JSON)
                .content(corpo(token(c), "outro@teste.invalid"))).andExpect(status().isBadRequest());
        mvc.perform(post("/cadastro/admin").contentType(MediaType.APPLICATION_JSON)
                .content(corpo(token(c), "autorizado@teste.invalid").replace("Senha@123", "fraca")))
                .andExpect(status().isBadRequest());
        assertNull(convites.findById(c.id()).orElseThrow().getUtilizadoEm());
        assertEquals(0, admins.count());
    }

    @Test void publicoNaoCriaAdminComRoleForjado() throws Exception {
        for (String perfil : List.of("responsavel", "profissional")) {
            mvc.perform(post("/cadastro/" + perfil).contentType(MediaType.APPLICATION_JSON).content("""
                    {"nome":"Comum","email":"%s@teste.invalid","senha":"Senha@123","cpf":"12345678901","role":"ADMIN","tipoPerfil":"ADMIN"}
                    """.formatted(perfil))).andExpect(status().isOk());
        }
        assertEquals(0, admins.count());
        mvc.perform(get("/api/admin/convites").header("Authorization", bearer(responsaveis.findByEmail("responsavel@teste.invalid").orElseThrow())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/convites").header("Authorization", bearer(profissionais.findByEmail("profissional@teste.invalid").orElseThrow())))
                .andExpect(status().isForbidden());
    }

    @Test void emitirListarRevogarRestritosSemExporHash() throws Exception {
        var a = admin(); var r = responsavel();
        mvc.perform(post("/api/admin/convites").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"nova@teste.invalid\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/convites").header("Authorization", bearer(r)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nova@teste.invalid\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/convites").header("Authorization", bearer(a)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nova@teste.invalid\"}")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.link").isNotEmpty()).andExpect(jsonPath("$.tokenHash").doesNotExist());
        var c = convites.findAll().getFirst();
        mvc.perform(get("/api/admin/convites").header("Authorization", bearer(a))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tokenHash").doesNotExist()).andExpect(jsonPath("$[0].link").doesNotExist());
        mvc.perform(delete("/api/admin/convites/" + c.getId()).header("Authorization", bearer(r))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/convites/" + c.getId()).header("Authorization", bearer(a))).andExpect(status().isNoContent());
        assertNotNull(convites.findById(c.getId()).orElseThrow().getRevogadoEm());
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.gerarPrimeiro("segundo@teste.invalid"));
    }

    @Test void adminCriaEditaEExcluiPostagemComAnexosECategorias() throws Exception {
        var a = admin();
        mvc.perform(post("/api/aprendizagem").header("Authorization", bearer(a)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Nova postagem\",\"conteudoHtml\":\"<p>Texto</p><script>alert(1)</script>\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.conteudoHtml").value("<p>Texto</p>"));
        var p = postagens.findAll().getFirst();
        Categoria cat = new Categoria(); cat.setNome("Categoria compartilhada"); em.persist(cat);
        p.getCategorias().add(cat);
        Anexo anexo = new Anexo(); anexo.setPostagem(p); anexo.setNomeOriginal("referencia.pdf"); p.getAnexos().add(anexo);
        postagens.saveAndFlush(p); Long anexoId = anexos.findAll().getFirst().getId();
        em.refresh(p); LocalDateTime criacao = p.getDataCriacao();
        mvc.perform(put("/api/aprendizagem/" + p.getId()).header("Authorization", bearer(a)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Editada\",\"conteudoHtml\":\"<p onclick='x()'>Novo texto</p>\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudoHtml").value("<p>Novo texto</p>"));
        em.flush(); em.clear();
        var editada = postagens.findById(p.getId()).orElseThrow();
        assertEquals(criacao, editada.getDataCriacao()); assertEquals(1, editada.getCategorias().size());
        assertTrue(anexos.existsById(anexoId));
        mvc.perform(delete("/api/aprendizagem/" + p.getId()).header("Authorization", bearer(a))).andExpect(status().isNoContent());
        em.clear(); assertFalse(postagens.existsById(p.getId())); assertFalse(anexos.existsById(anexoId));
        assertNotNull(em.find(Categoria.class, cat.getId()));
    }

    @Test void adminPodeEditarConteudoExistenteSemAmpliarAcessoPrivado() throws Exception {
        var a = admin(); var r = responsavel();
        Postagem p = new Postagem(); p.setTitulo("Postagem anterior"); p.setConteudoHtml("<p>Anterior</p>");
        p.setPublicadoAprendizagem(true); postagens.saveAndFlush(p);
        for (String method : List.of("POST", "PUT", "DELETE")) {
            String path = method.equals("POST") ? "/api/aprendizagem" : "/api/aprendizagem/" + p.getId();
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), path).header("Authorization", bearer(r))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Teste\",\"conteudoHtml\":\"<p>Teste</p>\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON)
                    .content("{}")).andExpect(status().isUnauthorized());
        }
        mvc.perform(put("/api/aprendizagem/" + p.getId()).header("Authorization", bearer(a)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Atualizada\",\"conteudoHtml\":\"<p>Atualizada</p>\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/aprendizagem").header("Authorization", bearer(a)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        for (String path : List.of("/api/perfil", "/api/perfil-responsavel", "/api/conta/foto")) {
            mvc.perform(get(path).header("Authorization", bearer(a))).andExpect(status().isForbidden());
        }
    }
}
