package br.com.neurohelp.tcc_backend.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.net.http.HttpClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.util.List;
import java.util.Map;

/** Seleção explícita do provedor HTTPS; mantém o contrato usado pela recuperação. */
@Service
public class EmailRecuperacaoService {
    private static final Logger log = LoggerFactory.getLogger(EmailRecuperacaoService.class);
    private final String apiKey;
    private final String remetente;
    private final RestClient client;
    private final String provedor;
    private final GmailRecuperacaoService gmail;
    public EmailRecuperacaoService(@Value("${app.email.resend-api-key:}") String apiKey,
                                   @Value("${app.email.from:}") String remetente,
                                   @Value("${app.email.provider:resend}") String provedor,
                                   GmailRecuperacaoService gmail) {
        this.apiKey = apiKey; this.remetente = remetente;
        this.provedor = provedor.trim().toLowerCase(java.util.Locale.ROOT); this.gmail = gmail;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(15));
        client = RestClient.builder().baseUrl("https://api.resend.com").requestFactory(factory).build();
        String nomeSeguro = List.of("gmail", "resend").contains(this.provedor) ? this.provedor : "desconhecido";
        log.info("Recuperação de senha: provedor={}, configurado={}.", nomeSeguro, configurado());
    }
    public boolean configurado() {
        return switch (provedor) {
            case "gmail" -> gmail.configurado();
            case "resend" -> !apiKey.isBlank() && !remetente.isBlank();
            default -> false;
        };
    }
    public void enviar(String destinatario, String codigo) {
        enviar(destinatario, codigo, false);
    }
    public void enviarConfirmacao(String destinatario, String codigo) {
        enviar(destinatario, codigo, true);
    }
    private void enviar(String destinatario, String codigo, boolean confirmacao) {
        if (!configurado()) throw new IllegalStateException("Provedor de e-mail não configurado.");
        if ("gmail".equals(provedor)) {
            if (confirmacao) gmail.enviarConfirmacao(destinatario, codigo); else gmail.enviar(destinatario, codigo);
            return;
        }
        client.post().uri("/emails").header("Authorization", "Bearer " + apiKey)
                .body(Map.of("from", remetente, "to", List.of(destinatario),
                        "subject", confirmacao ? "EspectroCare — confirme seu e-mail" : "EspectroCare — redefinição de senha",
                        "text", (confirmacao ? "Seu código para confirmar o e-mail e concluir o cadastro é: " : "Seu código para redefinir a senha é: ") + codigo
                                + "\n\nEle vale por 15 minutos e pode ser usado uma única vez. "
                                + (confirmacao ? "Informe-o na confirmação do cadastro no site ou aplicativo. " : "Informe-o na tela Recuperar senha do site ou aplicativo. ")
                                + (confirmacao ? "Se não iniciou este cadastro, ignore este e-mail. Nunca compartilhe o código." : "Se não solicitou esta alteração, ignore este e-mail. Nunca compartilhe o código.")))
                .retrieve().toBodilessEntity();
    }
}
