package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Entity.User.UsuarioAutenticavel;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.responsavelRepository;
import br.com.neurohelp.tcc_backend.Security.TokenService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FotoPerfilTest {
    @Autowired MockMvc mvc;
    @Autowired profissionalRepository profissionais;
    @Autowired responsavelRepository responsaveis;
    @Autowired TokenService tokens;
    @Autowired EntityManager em;
    UserProf prof;
    UserResp resp;

    @BeforeEach
    void dados() {
        prof = new UserProf();
        prof.setNome("Profissional foto"); prof.setEmail("foto-prof@teste.invalid");
        prof.setCpf("12345678901"); prof.setSenha("hash-teste");
        profissionais.saveAndFlush(prof);
        resp = new UserResp();
        resp.setNome("Responsável foto"); resp.setEmail("foto-resp@teste.invalid"); resp.setSenha("hash-teste");
        responsaveis.saveAndFlush(resp);
    }

    String bearer(UsuarioAutenticavel u) { return "Bearer " + tokens.gerarToken(u); }
    String imagem(int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, Color.RED.getRGB());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }
    String corpo(String photo) { return "{\"fotoPerfil\":\"" + photo + "\"}"; }

    @Test
    void novaContaComecaSemFotoEExigeLogin() throws Exception {
        mvc.perform(get("/api/conta/foto")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/conta/foto").contentType(MediaType.APPLICATION_JSON).content(corpo(imagem(32, 32))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/conta/foto").header("Authorization", bearer(resp)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fotoPerfil").isEmpty());
    }

    @Test
    void profissionalEResponsavelPodemSalvarERecuperarFotoDoBanco() throws Exception {
        for (UsuarioAutenticavel user : new UsuarioAutenticavel[]{prof, resp}) {
            String email = user.getEmail();
            mvc.perform(put("/api/conta/foto").header("Authorization", bearer(user))
                    .contentType(MediaType.APPLICATION_JSON).content(corpo(imagem(32, 48))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.fotoPerfil", startsWith("data:image/jpeg;base64,")));
            em.flush(); em.clear();
            UsuarioAutenticavel recarregado = user instanceof UserProf
                    ? profissionais.findByEmail(email).orElseThrow() : responsaveis.findByEmail(email).orElseThrow();
            mvc.perform(get("/api/conta/foto").header("Authorization", bearer(recarregado)))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.fotoPerfil", startsWith("data:image/jpeg;base64,")));
        }
    }

    @Test
    void idEnviadoNaoPodeAlterarOutraConta() throws Exception {
        UserResp outro = new UserResp();
        outro.setNome("Outra conta"); outro.setEmail("outra-foto@teste.invalid"); outro.setSenha("hash");
        responsaveis.saveAndFlush(outro);
        mvc.perform(put("/api/conta/foto").header("Authorization", bearer(resp))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + outro.getId() + ",\"fotoPerfil\":\"" + imagem(32, 32) + "\"}"))
                .andExpect(status().isOk());
        em.flush(); em.clear();
        assertNull(responsaveis.findByEmail(outro.getEmail()).orElseThrow().getFotoPerfil());
        assertNotNull(responsaveis.findByEmail(resp.getEmail()).orElseThrow().getFotoPerfil());
        assertNull(profissionais.findByEmail(prof.getEmail()).orElseThrow().getFotoPerfil());
    }

    @ParameterizedTest
    @ValueSource(strings = {"data:image/svg+xml;base64,PHN2Zy8+", "data:image/jpeg;base64,%%",
            "data:image/png;base64,YWJj", "https://exemplo.invalid/foto.png", ""})
    void rejeitaImagemInvalidaSemSobrescreverFoto(String photo) throws Exception {
        resp.setFotoPerfil(imagem(32, 32)); responsaveis.saveAndFlush(resp);
        String antes = resp.getFotoPerfil();
        mvc.perform(put("/api/conta/foto").header("Authorization", bearer(resp))
                .contentType(MediaType.APPLICATION_JSON).content(corpo(photo)))
                .andExpect(status().isBadRequest());
        assertEquals(antes, responsaveis.findByEmail(resp.getEmail()).orElseThrow().getFotoPerfil());
    }

    @Test
    void rejeitaDimensaoOuTamanhoExcessivo() throws Exception {
        for (String photo : new String[]{imagem(1025, 2), "data:image/jpeg;base64," + "A".repeat(180000),
                "data:image/jpeg;base64," + "A".repeat(175000)}) {
            mvc.perform(put("/api/conta/foto").header("Authorization", bearer(prof))
                    .contentType(MediaType.APPLICATION_JSON).content(corpo(photo)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void editarDadosAntigosPreservaFotoENaoMudaContratoDoApp() throws Exception {
        mvc.perform(put("/api/conta/foto").header("Authorization", bearer(prof))
                .contentType(MediaType.APPLICATION_JSON).content(corpo(imagem(32, 32))))
                .andExpect(status().isOk());
        mvc.perform(put("/api/perfil").header("Authorization", bearer(prof))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Atualizado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Atualizado"))
                .andExpect(jsonPath("$.fotoPerfil").doesNotExist());
        mvc.perform(get("/api/conta/foto").header("Authorization", bearer(prof)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fotoPerfil", startsWith("data:image/jpeg;base64,")));
        mvc.perform(get("/api/profissionais"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].fotoPerfil").doesNotExist());
    }

    @Test
    void fotoPublicaDoProfissionalRecarregaSemExporDadosPrivados() throws Exception {
        mvc.perform(put("/api/conta/foto").header("Authorization", bearer(prof))
                .contentType(MediaType.APPLICATION_JSON).content(corpo(imagem(32, 32))))
                .andExpect(status().isOk());
        em.flush(); em.clear();
        mvc.perform(get("/api/profissionais"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$[0].fotoPerfilUrl").value("/api/profissionais/" + prof.getId() + "/foto"))
                .andExpect(jsonPath("$[0].senha").doesNotExist()).andExpect(jsonPath("$[0].email").doesNotExist());
        mvc.perform(get("/api/profissionais/" + prof.getId() + "/foto"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/profissionais/999999/foto")).andExpect(status().isNotFound());
        mvc.perform(put("/api/conta/foto").header("Authorization", bearer(prof))
                .contentType(MediaType.APPLICATION_JSON).content(corpo(imagem(32, 32).replace("image/png", "image/jpeg"))))
                .andExpect(status().isBadRequest());
    }
}
