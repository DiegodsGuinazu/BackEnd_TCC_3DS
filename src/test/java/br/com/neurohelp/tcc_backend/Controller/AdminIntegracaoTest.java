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
                    {"nome":"Comum","email":"%s@teste.invalid","senha":"Senha@123","cpf":"52998224725","role":"ADMIN","tipoPerfil":"ADMIN"}
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
                .content(completa("{\"titulo\":\"Nova postagem\",\"conteudoHtml\":\"<p>Texto</p><script>alert(1)</script>\"}")))
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

    String imagemTeste() throws Exception {
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(40, 20,
                java.awt.image.BufferedImage.TYPE_INT_RGB), "png", output);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    }
    String completa(String json) throws Exception {
        return json.substring(0, json.length() - 1) + ",\"resumo\":\"Resumo educativo\","
                + "\"referencias\":\"https://example.org/fonte\",\"imagemData\":\"" + imagemTeste() + "\"}";
    }

    @Test void novaPostagemExigeImagemResumoEReferencias() throws Exception {
        var auth = bearer(admin());
        var minimal = "{\"titulo\":\"Teste\",\"conteudoHtml\":\"<p>Texto</p>\"}";
        mvc.perform(post("/api/aprendizagem").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(minimal)).andExpect(status().isBadRequest());
        for (String campo : List.of("imagemData", "resumo", "referencias")) {
            String payload = completa(minimal);
            String valor = campo.equals("imagemData") ? imagemTeste() : campo.equals("resumo") ? "Resumo educativo" : "https://example.org/fonte";
            payload = payload.replace("\"" + campo + "\":\"" + valor + "\"", "\"" + campo + "\":null");
            mvc.perform(post("/api/aprendizagem").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content(payload)).andExpect(status().isBadRequest());
        }
        assertEquals(0, postagens.count());
    }

    @Test void imagemResumoReferenciasPersistemEImagemPublicaNaoExpoeArtigo() throws Exception {
        var auth = bearer(admin());
        mvc.perform(post("/api/aprendizagem").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(completa("{\"titulo\":\"Nova\",\"conteudoHtml\":\"<p>Conteúdo</p>\"}")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.resumo").value("Resumo educativo"))
                .andExpect(jsonPath("$.referencias").value("https://example.org/fonte"))
                .andExpect(jsonPath("$.imagemData").doesNotExist());
        var id = postagens.findAll().getFirst().getId(); em.flush(); em.clear();
        mvc.perform(get("/api/aprendizagem")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].imagemUrl").value("/api/aprendizagem/" + id + "/imagem"))
                .andExpect(jsonPath("$[0].referencias").doesNotExist());
        var result = mvc.perform(get("/api/aprendizagem/" + id + "/imagem")).andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg")).andExpect(header().string("Cache-Control", "no-store")).andReturn();
        var image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
        assertEquals(40, image.getWidth()); assertEquals(20, image.getHeight());
        mvc.perform(get("/api/aprendizagem/" + id)).andExpect(status().isUnauthorized());
        var saved = postagens.findById(id).orElseThrow().getImagemData();
        mvc.perform(put("/api/aprendizagem/" + id).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Editada\",\"conteudoHtml\":\"<p>Novo</p>\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.referencias").value("https://example.org/fonte"));
        assertEquals(saved, postagens.findById(id).orElseThrow().getImagemData());
        mvc.perform(delete("/api/aprendizagem/" + id).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/aprendizagem/" + id + "/imagem")).andExpect(status().isNotFound());
    }

    @Test void recusaReferenciasPerigosasEImagemInvalida() throws Exception {
        var auth = bearer(admin());
        String valid = completa("{\"titulo\":\"Nova\",\"conteudoHtml\":\"<p>Conteúdo</p>\"}");
        for (String ref : List.of("javascript:alert(1)", "data:text/html,x", "/relativa", "https://user:senha@example.org")) {
            mvc.perform(post("/api/aprendizagem").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content(valid.replace("https://example.org/fonte", ref))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/aprendizagem").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(valid.replace(imagemTeste(), "data:image/png;base64,aW52YWxpZA=="))).andExpect(status().isBadRequest());
        assertEquals(0, postagens.count());
    }

    @Test void profissionalTambemNaoPodeCriarEditarOuExcluir() throws Exception {
        UserProf profissional = new UserProf(); profissional.setNome("Profissional"); profissional.setEmail("prof@teste.invalid");
        profissional.setSenha(encoder.encode("Senha@123")); profissional.setCpf("52998224725"); profissionais.saveAndFlush(profissional);
        for (String method : List.of("POST", "PUT", "DELETE")) {
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), method.equals("POST") ? "/api/aprendizagem" : "/api/aprendizagem/1")
                    .header("Authorization", bearer(profissional)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
    }
}
