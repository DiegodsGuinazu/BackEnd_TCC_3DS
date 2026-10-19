package br.com.neurohelp.tcc_backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

/** Gmail API por HTTPS; OAuth é autorizado uma vez pelo operador em sua máquina. */
@Service
public class GmailRecuperacaoService {
    static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    static final String SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";
    private static final Pattern ENDERECO = Pattern.compile("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?");
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String remetente;
    private final RestClient client;
    private final Clock clock;
    private String accessToken;
    private Instant renovarEm = Instant.EPOCH;

    @Autowired
    public GmailRecuperacaoService(@Value("${app.email.gmail.client-id:}") String clientId,
            @Value("${app.email.gmail.client-secret:}") String clientSecret,
            @Value("${app.email.gmail.refresh-token:}") String refreshToken,
            @Value("${app.email.gmail.sender:}") String remetente) {
        this(clientId, clientSecret, refreshToken, remetente, builder(), Clock.systemUTC());
    }
    GmailRecuperacaoService(String clientId, String clientSecret, String refreshToken, String remetente,
            RestClient.Builder builder, Clock clock) {
        this.clientId = clientId; this.clientSecret = clientSecret; this.refreshToken = refreshToken;
        this.remetente = remetente; this.client = builder.build(); this.clock = clock;
    }
    private static RestClient.Builder builder() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder().requestFactory(factory);
    }
    public boolean configurado() {
        return !clientId.isBlank() && !clientSecret.isBlank() && !refreshToken.isBlank()
                && remetente.length() <= 254 && ENDERECO.matcher(remetente).matches();
    }
    public void enviar(String destinatario, String codigo) {
        if (!configurado()) throw new IllegalStateException("Envio Gmail não configurado.");
        if (destinatario == null || destinatario.length() > 254 || !ENDERECO.matcher(destinatario).matches()
                || codigo == null || !codigo.matches("[0-9]{8}"))
            throw new IllegalArgumentException("Dados de envio inválidos.");
        try {
            String token = obterAccessToken();
            client.post().uri(SEND_URL).headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON).body(Map.of("raw", mensagem(destinatario, codigo)))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            if (e instanceof RestClientResponseException http && http.getStatusCode().value() == 401) invalidarToken();
            // Exceções HTTP podem conter corpo, tokens ou mensagens do provedor. Não propagar a causa.
            throw new IllegalStateException("Falha no envio Gmail. Verifique a autorização e os limites da conta.");
        }
    }
    private synchronized String obterAccessToken() {
        if (accessToken != null && clock.instant().isBefore(renovarEm)) return accessToken;
        var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", clientId); form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken); form.add("grant_type", "refresh_token");
        TokenResponse resposta = client.post().uri(TOKEN_URL).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(TokenResponse.class);
        if (resposta == null || resposta.access_token() == null
                || !resposta.access_token().matches("[A-Za-z0-9._~+/=-]{1,4096}")
                || !"Bearer".equalsIgnoreCase(resposta.token_type()) || resposta.expires_in() == null
                || resposta.expires_in() <= 0 || resposta.expires_in() > 86400)
            throw new IllegalStateException("Resposta de autorização Gmail inválida.");
        accessToken = resposta.access_token();
        renovarEm = clock.instant().plusSeconds(Math.max(0, resposta.expires_in() - 60));
        return accessToken;
    }
    private synchronized void invalidarToken() { accessToken = null; renovarEm = Instant.EPOCH; }
    private String mensagem(String destinatario, String codigo) {
        String assunto = Base64.getEncoder().encodeToString("EspectroCare — redefinição de senha".getBytes(StandardCharsets.UTF_8));
        String texto = "Seu código para redefinir a senha é: " + codigo
                + "\n\nEle vale por 15 minutos e pode ser usado uma única vez. "
                + "Informe-o na tela Recuperar senha do site ou aplicativo. "
                + "Se não solicitou esta alteração, ignore este e-mail. Nunca compartilhe o código.";
        String corpo = Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(texto.getBytes(StandardCharsets.UTF_8));
        String mime = "From: EspectroCare <" + remetente + ">\r\nTo: <" + destinatario + ">\r\n"
                + "Subject: =?UTF-8?B?" + assunto + "?=\r\nMIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n" + corpo + "\r\n";
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mime.getBytes(StandardCharsets.UTF_8));
    }
    // Impede exibição acidental de credenciais se alguém inspecionar a resposta.
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(String access_token, Long expires_in, String token_type) {
        @Override public String toString() { return "TokenResponse[conteúdo protegido]"; }
    }
}
