package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Entity.Postagem.Categoria;
import br.com.neurohelp.tcc_backend.Entity.Postagem.Postagem;
import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Entity.User.UsuarioAutenticavel;
import br.com.neurohelp.tcc_backend.Repository.PostagemRepository;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.responsavelRepository;
import br.com.neurohelp.tcc_backend.Security.TokenService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AreasProtegidasTest {
    @Autowired MockMvc mvc;
    @Autowired profissionalRepository profissionais;
    @Autowired responsavelRepository responsaveis;
    @Autowired PostagemRepository postagens;
    @Autowired TokenService tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager entityManager;
    @Value("${api.security.token.secret}") String secret;

    UserProf profissional;
    UserResp responsavel;
    Postagem publicado;
    Postagem rascunho;

    @BeforeEach
    void prepararDados() {
        profissional = new UserProf();
        profissional.setNome("Profissional de teste");
        profissional.setEmail("profissional@teste.invalid");
        profissional.setCpf("22222222222");
        profissional.setSenha(encoder.encode("Senha@123"));
        profissional.setTelefone("11999999999");
        profissional.setEstado("SP");
        profissional.setBio("Apresentação profissional");
        profissional.setNumRegistro("REG-TESTE");
        profissionais.saveAndFlush(profissional);

        responsavel = new UserResp();
        responsavel.setNome("Responsável de teste");
        responsavel.setEmail("responsavel@teste.invalid");
        responsavel.setCpf("33333333333");
        responsavel.setSenha(encoder.encode("Senha@123"));
        responsaveis.saveAndFlush(responsavel);

        Categoria categoria = new Categoria();
        categoria.setNome("TEA");
        entityManager.persist(categoria);
        publicado = new Postagem();
        publicado.setTitulo("Artigo da equipe");
        publicado.setConteudoHtml("<p>Conteúdo de aprendizagem</p>");
        publicado.setDataCriacao(LocalDateTime.of(2026, 9, 30, 12, 0));
        publicado.setPublicadoAprendizagem(true);
        publicado.setCategorias(Set.of(categoria));
        postagens.saveAndFlush(publicado);

        rascunho = new Postagem();
        rascunho.setTitulo("Conteúdo ainda não aprovado");
        postagens.saveAndFlush(rascunho);
    }

    String bearer(UsuarioAutenticavel usuario) {
        return "Bearer " + tokens.gerarToken(usuario);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/me", "/api/profissionais", "/api/profissionais/1",
            "/api/aprendizagem", "/api/aprendizagem/1", "/api/perfil", "/api/perfil-responsavel"})
    void semTokenRetorna401Json(String rota) throws Exception {
        mvc.perform(get(rota)).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer token-invalido", "Bearer null", "Bearer undefined", "Basic abc", "Bearer "})
    void tokenInvalidoRetorna401(String header) throws Exception {
        mvc.perform(get("/api/profissionais").header("Authorization", header))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoOuAssinadoComOutraChaveRetorna401() throws Exception {
        String expirado = JWT.create().withIssuer("API EspectroCare").withSubject(profissional.getEmail())
                .withExpiresAt(Instant.now().minusSeconds(60)).sign(Algorithm.HMAC256(secret));
        String falso = JWT.create().withIssuer("API EspectroCare").withSubject(profissional.getEmail())
                .withExpiresAt(Instant.now().plusSeconds(60)).sign(Algorithm.HMAC256("outra-chave-de-teste"));
        for (String token : new String[]{expirado, falso}) {
            mvc.perform(get("/api/aprendizagem").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void usuarioExcluidoNaoPodeContinuarUsandoToken() throws Exception {
        String authorization = bearer(responsavel);
        responsaveis.delete(responsavel);
        responsaveis.flush();
        mvc.perform(get("/api/auth/me").header("Authorization", authorization))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ambosPerfisPodemConsultarAsAreasSemExporDadosPrivados() throws Exception {
        for (UsuarioAutenticavel usuario : new UsuarioAutenticavel[]{profissional, responsavel}) {
            mvc.perform(get("/api/profissionais").header("Authorization", bearer(usuario)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(String.valueOf(profissional.getId())))
                    .andExpect(jsonPath("$[0].nome").value(profissional.getNome()))
                    .andExpect(jsonPath("$[0].cpf").doesNotExist())
                    .andExpect(jsonPath("$[0].senha").doesNotExist())
                    .andExpect(jsonPath("$[0].email").doesNotExist())
                    .andExpect(jsonPath("$[0].authorities").doesNotExist());
            mvc.perform(get("/api/profissionais/" + profissional.getId()).header("Authorization", bearer(usuario)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.numRegistro").value("REG-TESTE"))
                    .andExpect(jsonPath("$.cpf").doesNotExist()).andExpect(jsonPath("$.senha").doesNotExist());
            mvc.perform(get("/api/aprendizagem").header("Authorization", bearer(usuario)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].titulo").value("Artigo da equipe"))
                    .andExpect(jsonPath("$[0].categorias[0]").value("TEA"))
                    .andExpect(jsonPath("$[0].conteudoHtml").doesNotExist());
            mvc.perform(get("/api/aprendizagem/" + publicado.getId()).header("Authorization", bearer(usuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudoHtml").value(publicado.getConteudoHtml()))
                    .andExpect(jsonPath("$.anexos").doesNotExist());
        }
    }

    @Test
    void rascunhosEIdsInexistentesRetornam404() throws Exception {
        for (String rota : new String[]{"/api/aprendizagem/" + rascunho.getId(),
                "/api/aprendizagem/999999", "/api/profissionais/999999"}) {
            mvc.perform(get(rota).header("Authorization", bearer(responsavel)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void meRetornaTipoPerfilSemCpfOuSenha() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(profissional)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipoPerfil").value("PROFISSIONAL"))
                .andExpect(jsonPath("$.email").value(profissional.getEmail()))
                .andExpect(jsonPath("$.cpf").doesNotExist()).andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(responsavel)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipoPerfil").value("RESPONSAVEL"));
    }

    @Test
    void perfilPrivadoMantemCamposCorretosENaoPermiteTipoErrado() throws Exception {
        mvc.perform(get("/api/perfil").header("Authorization", bearer(profissional)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.telefone").value("11999999999"))
                .andExpect(jsonPath("$.estado").value("SP"));
        mvc.perform(get("/api/perfil-responsavel").header("Authorization", bearer(responsavel)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cpf").value(responsavel.getCpf()));
        mvc.perform(get("/api/perfil").header("Authorization", bearer(responsavel)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/perfil-responsavel").header("Authorization", bearer(profissional)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/perfil").header("Authorization", bearer(responsavel))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Não autorizado\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/perfil-responsavel").header("Authorization", bearer(profissional))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Não autorizado\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void atualizacaoDoProprioPerfilContinuaFuncionando() throws Exception {
        mvc.perform(put("/api/perfil").header("Authorization", bearer(profissional))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Nome atualizado\",\"telefone\":\"11888888888\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Nome atualizado"))
                .andExpect(jsonPath("$.telefone").value("11888888888"))
                .andExpect(jsonPath("$.estado").value("SP"));
        mvc.perform(put("/api/perfil-responsavel").header("Authorization", bearer(responsavel))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Responsável atualizado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Responsável atualizado"));
    }

    @Test
    void leituraNaoPermitePublicacaoPorUsuariosComuns() throws Exception {
        for (UsuarioAutenticavel usuario : new UsuarioAutenticavel[]{profissional, responsavel}) {
            mvc.perform(post("/publicacao/salvar").header("Authorization", bearer(usuario))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"publicadoAprendizagem\":true}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void corsAutorizaOrigemConhecidaERejeitaDesconhecida() throws Exception {
        mvc.perform(options("/api/profissionais").header("Origin", "https://neuro-help-psi.vercel.app")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://neuro-help-psi.vercel.app"));
        mvc.perform(options("/auth/login").header("Origin", "https://origem-desconhecida.invalid")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/profissionais").header("Origin", "https://neuro-help-psi.vercel.app"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://neuro-help-psi.vercel.app"));
    }

    @Test
    void cadastrosContinuamPublicosELoginDoResponsavelGeraToken() throws Exception {
        mvc.perform(post("/cadastro/profissional").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Novo profissional\",\"email\":\"novo-prof@teste.invalid\",\"cpf\":\"44444444444\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/cadastro/responsavel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Novo responsável\",\"email\":\"novo-resp@teste.invalid\",\"cpf\":\"55555555555\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"novo-resp@teste.invalid\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.email").value("novo-resp@teste.invalid"));
    }

    @Test
    void cadastroNaoPodeSobrescreverContaExistentePorId() throws Exception {
        mvc.perform(post("/cadastro/profissional").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + profissional.getId() + ",\"nome\":\"Outra conta\","
                        + "\"email\":\"outra@teste.invalid\",\"cpf\":\"66666666666\",\"senha\":\"Outra@123\"}"))
                .andExpect(status().isOk());
        assertEquals(2, profissionais.count());
        assertEquals("profissional@teste.invalid", profissionais.findByEmail("profissional@teste.invalid").orElseThrow().getEmail());
    }

    @Test
    void cadastroIncompletoRetorna400() throws Exception {
        mvc.perform(post("/cadastro/responsavel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalido\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cadastroRejeitaEmailJaExistenteNoOutroTipoDePerfil() throws Exception {
        mvc.perform(post("/cadastro/responsavel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Outra conta\",\"email\":\"profissional@teste.invalid\",\"cpf\":\"77777777777\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isConflict());
        assertEquals(1, responsaveis.count());
    }

    @Test
    void tokenAntigoContinuaValidoQuandoEmailNaoEAmbiguo() throws Exception {
        String legado = JWT.create().withIssuer("API EspectroCare").withSubject(responsavel.getEmail())
                .withExpiresAt(Instant.now().plusSeconds(60)).sign(Algorithm.HMAC256(secret));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + legado))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipoPerfil").value("RESPONSAVEL"));
    }

    @Test
    void emailsLegadosDuplicadosNaoTrocamAIdentidadeDoToken() throws Exception {
        String tokenResponsavel = bearer(responsavel);
        UserProf duplicado = new UserProf();
        duplicado.setNome("Profissional com email legado duplicado");
        duplicado.setEmail(responsavel.getEmail());
        duplicado.setCpf("88888888888");
        duplicado.setSenha(encoder.encode("Senha@123"));
        profissionais.saveAndFlush(duplicado);

        mvc.perform(get("/api/auth/me").header("Authorization", tokenResponsavel))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipoPerfil").value("RESPONSAVEL"));
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(duplicado)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipoPerfil").value("PROFISSIONAL"));
        String legado = JWT.create().withIssuer("API EspectroCare").withSubject(responsavel.getEmail())
                .withExpiresAt(Instant.now().plusSeconds(60)).sign(Algorithm.HMAC256(secret));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + legado))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"responsavel@teste.invalid\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDuraDuasHorasIndependenteDoFusoDoServidor() {
        Instant antes = Instant.now();
        Instant expira = JWT.decode(tokens.gerarToken(profissional)).getExpiresAtAsInstant();
        long segundos = Duration.between(antes, expira).getSeconds();
        assertTrue(segundos >= 7198 && segundos <= 7200);
    }

    @Test
    void responsavelPodeCadastrarSemCpfEComDadosNormalizados() throws Exception {
        mvc.perform(post("/cadastro/responsavel").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Sem CPF","email":"semcpf@teste.invalid","senha":"Senha@123",
                         "cpf":null,"telefone":"(11) 98765-4321","cidade":"São Paulo"}
                        """))
                .andExpect(status().isOk());

        UserResp salvo = responsaveis.findByEmail("semcpf@teste.invalid").orElseThrow();
        assertEquals(null, salvo.getCpf());
        assertEquals("11987654321", salvo.getTelefone());
        assertEquals("São Paulo", salvo.getCidade());
    }

    @Test
    void doisResponsaveisPodemTerCpfNull() throws Exception {
        for (int i = 1; i <= 2; i++) {
            mvc.perform(post("/cadastro/responsavel").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"nome":"Resp %d","email":"resp-null-%d@teste.invalid","senha":"Senha@123","cpf":null}
                            """.formatted(i, i)))
                    .andExpect(status().isOk());
        }

        assertEquals(2, responsaveis.findAll().stream().filter(usuario -> usuario.getCpf() == null).count());
    }

    @Test
    void cadastroNormalizaCpfTelefoneEmailCidadeEFormacao() throws Exception {
        mvc.perform(post("/cadastro/profissional").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Profissional","email":"PROF-NORMALIZA@TESTE.INVALID ","senha":"Senha@123",
                         "cpf":"123.456.789-00","telefone":"(11) 3456-7890","estado":" São Paulo ",
                         "cidade":" Campinas ","formacao":" Psicologia "}
                        """))
                .andExpect(status().isOk());

        UserProf salvo = profissionais.findByEmail("prof-normaliza@teste.invalid").orElseThrow();
        assertEquals("12345678900", salvo.getCpf());
        assertEquals("1134567890", salvo.getTelefone());
        assertEquals("São Paulo", salvo.getEstado());
        assertEquals("Campinas", salvo.getCidade());
        assertEquals("Psicologia", salvo.getFormacao());
    }

    @Test
    void cadastroRejeitaCpfTelefoneESenhaInvalidos() throws Exception {
        mvc.perform(post("/cadastro/profissional").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Sem CPF","email":"sem-cpf-prof@teste.invalid","senha":"Senha@123"}
                        """))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/cadastro/responsavel").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"CPF curto","email":"cpf-curto@teste.invalid","senha":"Senha@123","cpf":"1234567890"}
                        """))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/cadastro/responsavel").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Telefone longo","email":"tel-longo@teste.invalid","senha":"Senha@123",
                         "telefone":"119999999999"}
                        """))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/cadastro/responsavel").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Senha fraca","email":"senha-fraca@teste.invalid","senha":"senha123"}
                        """))
                .andExpect(status().isBadRequest());
    }
}
