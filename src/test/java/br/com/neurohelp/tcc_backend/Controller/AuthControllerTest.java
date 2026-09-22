package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private profissionalRepository profissionalRepository;

    @Test
    public void loginComSenhaCorretaDeveRetornarToken() throws Exception {
        UserProf usuario = new UserProf();
        usuario.setEmail("teste@teste.com");
        usuario.setSenha(passwordEncoder.encode("senha123"));
        usuario.setCpf("00000000000");
        profissionalRepository.save(usuario);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"teste@teste.com","senha":"senha123"}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    public void loginComSenhaErradaDeveRetornar401() throws Exception {
        UserProf usuario = new UserProf();
        usuario.setEmail("teste2@teste.com");
        usuario.setSenha(passwordEncoder.encode("senha123"));
        usuario.setCpf("11111111111");
        profissionalRepository.save(usuario);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"teste2@teste.com","senha":"errada"}
                        """))
                .andExpect(status().isUnauthorized());
    }
}