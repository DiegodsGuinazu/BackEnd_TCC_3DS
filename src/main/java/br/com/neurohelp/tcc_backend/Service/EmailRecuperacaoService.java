package br.com.neurohelp.tcc_backend.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** HTTPS evita depender das portas SMTP, indisponíveis em alguns planos de hospedagem. */
@Service
public class EmailRecuperacaoService {
    private final String apiKey;
    private final String remetente;
    private final RestClient client;
    public EmailRecuperacaoService(@Value("${app.email.api-key:}") String apiKey,
                                   @Value("${app.email.from:}") String remetente) {
        this.apiKey = apiKey;
        this.remetente = remetente;
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(10));
        client = RestClient.builder().requestFactory(factory).baseUrl("https://api.resend.com").build();
    }
    public boolean configurado() { return !apiKey.isBlank() && !remetente.isBlank(); }
    public boolean enviar(String destinatario, String codigo) {
        try {
            client.post().uri("/emails").header("Authorization", "Bearer " + apiKey)
                .body(Map.of("from", remetente, "to", List.of(destinatario),
                    "subject", "EspectroCare — alteração de senha",
                    "text", "Seu código para alterar a senha é: " + codigo
                        + "\nEle expira em 15 minutos e só pode ser usado uma vez. Digite-o na tela de recuperação do site ou aplicativo."
                        + "\nSe você não solicitou, ignore esta mensagem. Nunca compartilhe este código."))
                .retrieve().toBodilessEntity();
            return true;
        } catch (org.springframework.web.client.RestClientException e) {
            // Não registrar corpo do provedor, destinatário, código ou credenciais.
            return false;
        }
    }
}
