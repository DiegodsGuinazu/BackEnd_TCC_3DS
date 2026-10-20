package br.com.neurohelp.tcc_backend.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(OutputCaptureExtension.class)
class GmailRecuperacaoServiceTest {
    private MockRestServiceServer server;
    private GmailRecuperacaoService gmail;
    private final MutableClock clock = new MutableClock();
    @BeforeEach void preparar() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        gmail = new GmailRecuperacaoService("client-teste", "segredo-teste", "refresh-teste",
                "espectrocare@gmail.com", builder, clock);
    }
    void esperarToken(String token) {
        server.expect(requestTo(GmailRecuperacaoService.TOKEN_URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=refresh_token")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("client_id=client-teste")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("client_secret=segredo-teste")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("refresh_token=refresh-teste")))
                .andRespond(withSuccess("{\"access_token\":\"" + token + "\",\"expires_in\":3600,\"token_type\":\"Bearer\",\"scope\":\"gmail.send\"}", MediaType.APPLICATION_JSON));
    }
    void esperarEnvio(String token) {
        server.expect(requestTo(GmailRecuperacaoService.SEND_URL))
                .andExpect(header("Authorization", "Bearer " + token))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    String raw = new JsonMapper().readTree(body).get("raw").asString();
                    assertFalse(raw.contains("="));
                    String mime = new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8);
                    assertTrue(mime.startsWith("From: EspectroCare <espectrocare@gmail.com>\r\nTo: <pessoa@teste.invalid>\r\n"));
                    assertTrue(mime.contains("Content-Type: text/plain; charset=UTF-8"));
                    String texto = new String(Base64.getMimeDecoder().decode(mime.split("\r\n\r\n", 2)[1]), StandardCharsets.UTF_8);
                    assertTrue(texto.contains("01234567"));
                    assertTrue(texto.contains("15 minutos"));
                    assertFalse(mime.contains("refresh-teste"));
                    assertFalse(mime.contains("segredo-teste"));
                }).andRespond(withSuccess("{\"id\":\"mensagem-teste\"}", MediaType.APPLICATION_JSON));
    }
    @Test void enviaMimeEReutilizaAccessToken() {
        esperarToken("access-teste"); esperarEnvio("access-teste"); esperarEnvio("access-teste");
        gmail.enviar("pessoa@teste.invalid", "01234567");
        gmail.enviar("pessoa@teste.invalid", "01234567");
        server.verify();
    }
    @Test void renovaAntesDaExpiracao() {
        esperarToken("access-1"); esperarEnvio("access-1");
        esperarToken("access-2"); esperarEnvio("access-2");
        gmail.enviar("pessoa@teste.invalid", "01234567");
        clock.agora = clock.agora.plusSeconds(3541);
        gmail.enviar("pessoa@teste.invalid", "01234567");
        server.verify();
    }
    @Test void recusaInjecaoDeCabecalhoECodigoInvalidoSemFazerHttp() {
        assertThrows(IllegalArgumentException.class, () -> gmail.enviar("pessoa@teste.invalid\r\nBcc: outro@teste.invalid", "01234567"));
        assertThrows(IllegalArgumentException.class, () -> gmail.enviar("pessoa@teste.invalid", "123\r\nX-Test: valor"));
        server.verify();
    }
    @Test void erroDeTokenNaoExpoeCorpoOuCausa() {
        server.expect(requestTo(GmailRecuperacaoService.TOKEN_URL))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("segredo-teste refresh-teste access-teste pessoa@teste.invalid").contentType(MediaType.TEXT_PLAIN));
        var erro = assertThrows(IllegalStateException.class, () -> gmail.enviar("pessoa@teste.invalid", "01234567"));
        assertNull(erro.getCause());
        assertFalse(erro.getMessage().contains("segredo-teste"));
        assertFalse(erro.getMessage().contains("pessoa@teste.invalid"));
        server.verify();
    }
    @Test void respostaDeTokenIncompletaNaoEnvia() {
        server.expect(requestTo(GmailRecuperacaoService.TOKEN_URL))
            .andRespond(withSuccess("{\"access_token\":\"access-teste\",\"expires_in\":0,\"token_type\":\"Bearer\"}", MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class, () -> gmail.enviar("pessoa@teste.invalid", "01234567"));
        server.verify();
    }
    @Test void erro401DescartaTokenSemReenviarMensagemAutomaticamente() {
        esperarToken("access-1");
        server.expect(requestTo(GmailRecuperacaoService.SEND_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        esperarToken("access-2"); esperarEnvio("access-2");
        assertThrows(IllegalStateException.class, () -> gmail.enviar("pessoa@teste.invalid", "01234567"));
        gmail.enviar("pessoa@teste.invalid", "01234567");
        server.verify();
    }
    @Test void configuracaoIncompletaNaoFazHttp() {
        var incompleto = new GmailRecuperacaoService("", "", "", "", RestClient.builder(), clock);
        assertFalse(incompleto.configurado());
        assertThrows(IllegalStateException.class, () -> incompleto.enviar("pessoa@teste.invalid", "01234567"));
    }
    @Test void respostaSensivelTemToStringProtegido() {
        String texto = new GmailRecuperacaoService.TokenResponse("access-teste", 3600L, "Bearer").toString();
        assertFalse(texto.contains("access-teste"));
    }
    @ParameterizedTest
    @ValueSource(strings = {"invalid_grant", "invalid_client"})
    void identificaErroOAuthSemVazarCredenciais(String motivo, CapturedOutput output) {
        server.expect(requestTo(GmailRecuperacaoService.TOKEN_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"" + motivo + "\",\"error_description\":\"segredo-teste refresh-teste access-teste pessoa@teste.invalid 01234567\"}"));
        var erro = assertThrows(IllegalStateException.class, () -> gmail.enviar("pessoa@teste.invalid", "01234567"));
        assertNull(erro.getCause());
        assertTrue(output.getAll().contains("etapa=autorizacao, http=400, motivo=" + motivo));
        verificarPrivacidade(output);
        server.verify();
    }
    @Test void identificaApiDesativadaSemRegistrarMensagemOuMetadados(CapturedOutput output) {
        esperarToken("access-teste");
        server.expect(requestTo(GmailRecuperacaoService.SEND_URL))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"status\":\"PERMISSION_DENIED\",\"message\":\"pessoa@teste.invalid\","
                                + "\"details\":[{\"reason\":\"SERVICE_DISABLED\",\"metadata\":{\"secret\":\"refresh-teste\"}}]}}"));
        assertThrows(IllegalStateException.class, () -> gmail.enviar("pessoa@teste.invalid", "01234567"));
        assertTrue(output.getAll().contains("etapa=envio, http=403, motivo=SERVICE_DISABLED"));
        verificarPrivacidade(output);
        server.verify();
    }
    @Test void identificaPermissaoInsuficiente(CapturedOutput output) {
        esperarToken("access-teste");
        server.expect(requestTo(GmailRecuperacaoService.SEND_URL))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"errors\":[{\"reason\":\"insufficientPermissions\"}]}}"));
        assertThrows(IllegalStateException.class, () -> gmail.enviar("pessoa@teste.invalid", "01234567"));
        assertTrue(output.getAll().contains("etapa=envio, http=403, motivo=insufficientPermissions"));
        verificarPrivacidade(output);
        server.verify();
    }
    @ParameterizedTest
    @ValueSource(strings = {"{\"error\":\"refresh-teste\"}", "segredo-teste", "{\"error\":null}", "{\"error\":{\"errors\":[null],\"status\":\"pessoa@teste.invalid\"}}"})
    void omiteErrosDesconhecidosOuCorposInvalidos(String corpo, CapturedOutput output) {
        server.expect(requestTo(GmailRecuperacaoService.TOKEN_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body(corpo).contentType(MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class, () -> gmail.enviar("pessoa@teste.invalid", "01234567"));
        assertTrue(output.getAll().contains("etapa=autorizacao, http=400, motivo=nao_identificado"));
        verificarPrivacidade(output);
        server.verify();
    }
    @Test void distingueFalhaDeConexaoSemImprimirCausa(CapturedOutput output) {
        server.expect(requestTo(GmailRecuperacaoService.TOKEN_URL))
                .andRespond(request -> { throw new java.io.IOException("segredo-teste refresh-teste"); });
        assertThrows(IllegalStateException.class, () -> gmail.enviar("pessoa@teste.invalid", "01234567"));
        assertTrue(output.getAll().contains("etapa=autorizacao, http=0, motivo=falha_conexao"));
        verificarPrivacidade(output);
        server.verify();
    }
    private static void verificarPrivacidade(CapturedOutput output) {
        for (String privado : new String[]{"client-teste", "segredo-teste", "refresh-teste", "access-teste", "pessoa@teste.invalid", "01234567"})
            assertFalse(output.getAll().contains(privado), "Log expôs dado privado");
    }
    static class MutableClock extends Clock {
        Instant agora = Instant.parse("2026-10-19T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }
}
