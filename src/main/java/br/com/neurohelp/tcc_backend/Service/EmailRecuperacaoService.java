package br.com.neurohelp.tcc_backend.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.time.Duration;
import java.net.http.HttpClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.util.List;
import java.util.Map;

/** API HTTPS da Resend; nenhuma credencial fica no site ou no aplicativo. */
@Service
public class EmailRecuperacaoService {
    private final String apiKey;
    private final String remetente;
    private final RestClient client;
    public EmailRecuperacaoService(@Value("${app.email.resend-api-key:}") String apiKey,
                                   @Value("${app.email.from:}") String remetente) {
        this.apiKey = apiKey; this.remetente = remetente;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(15));
        client = RestClient.builder().baseUrl("https://api.resend.com").requestFactory(factory).build();
    }
    public boolean configurado() { return !apiKey.isBlank() && !remetente.isBlank(); }
    public void enviar(String destinatario, String codigo) {
        client.post().uri("/emails").header("Authorization", "Bearer " + apiKey)
                .body(Map.of("from", remetente, "to", List.of(destinatario),
                        "subject", "EspectroCare — redefinição de senha",
                        "text", "Seu código para redefinir a senha é: " + codigo
                                + "\n\nEle vale por 15 minutos e pode ser usado uma única vez. "
                                + "Informe-o na tela Recuperar senha do site ou aplicativo. "
                                + "Se não solicitou esta alteração, ignore este e-mail. Nunca compartilhe o código."))
                .retrieve().toBodilessEntity();
    }
}
