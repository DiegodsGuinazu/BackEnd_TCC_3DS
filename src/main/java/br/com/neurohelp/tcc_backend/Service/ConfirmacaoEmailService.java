package br.com.neurohelp.tcc_backend.Service;

import br.com.neurohelp.tcc_backend.Entity.User.ConfirmacaoEmail;
import br.com.neurohelp.tcc_backend.Repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

/** Confirma a posse do endereço antes de criar uma conta. Não altera usuários existentes. */
@Service
public class ConfirmacaoEmailService {
    private final ConfirmacaoEmailRepository codigos;
    private final profissionalRepository profissionais;
    private final responsavelRepository responsaveis;
    private final AdminRepository admins;
    private final EmailRecuperacaoService emails;
    private final RecuperacaoLimites limites;
    private final ApplicationEventPublisher eventos;
    private final String segredo;
    private final SecureRandom random = new SecureRandom();
    public ConfirmacaoEmailService(ConfirmacaoEmailRepository codigos, profissionalRepository profissionais,
            responsavelRepository responsaveis, AdminRepository admins, EmailRecuperacaoService emails,
            RecuperacaoLimites limites, ApplicationEventPublisher eventos,
            @Value("${api.security.token.secret}") String segredo) {
        this.codigos = codigos; this.profissionais = profissionais; this.responsaveis = responsaveis;
        this.admins = admins; this.emails = emails; this.limites = limites; this.eventos = eventos; this.segredo = segredo;
    }
    @Transactional
    public void solicitar(String informado, String origem) {
        if (!emails.configurado()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Confirmação de e-mail indisponível. Tente novamente mais tarde.");
        String email = normalizar(informado);
        limites.verificar("cadastro-envio-ip:" + hash(origem), 20);
        limites.verificar("cadastro-envio-email:" + hash(email), 3);
        // Resposta idêntica para endereço existente; nunca enviar códigos de cadastro para essas contas.
        if (profissionais.findByEmailIgnoreCase(email).isPresent() || responsaveis.findByEmailIgnoreCase(email).isPresent()
                || admins.findByEmailIgnoreCase(email).isPresent()) return;
        String chave = hash(email);
        var codigo = codigos.buscarComBloqueio(chave).orElseGet(ConfirmacaoEmail::new);
        Instant agora = Instant.now();
        if (codigo.getEnviadoEm() != null && codigo.getEnviadoEm().plusSeconds(60).isAfter(agora)) return;
        String valor = String.format(Locale.ROOT, "%08d", random.nextInt(100_000_000));
        codigo.setEmailHash(chave); codigo.setCodigoHash(hash(email + ":" + valor));
        codigo.setEnviadoEm(agora); codigo.setExpiraEm(agora.plusSeconds(900));
        codigo.setTentativas(0); codigo.setUtilizado(false);
        // O índice único resolve corridas na primeira solicitação sem enviar dois códigos válidos.
        codigos.saveAndFlush(codigo);
        eventos.publishEvent(new RecuperacaoEmailListener.ConfirmacaoCadastro(email, valor));
    }
    // Consumo e criação da conta pertencem à MESMA transação: falha ao salvar não perde o código.
    // Tentativas inválidas devem ser confirmadas no banco mesmo quando retornamos 422.
    @Transactional(propagation = Propagation.MANDATORY, noRollbackFor = ResponseStatusException.class)
    public void consumir(String informado, String valor) {
        String email = normalizar(informado);
        if (valor == null || !valor.matches("[0-9]{8}")) throw new EmailNaoConfirmadoException();
        var codigo = codigos.buscarComBloqueio(hash(email)).orElse(null);
        if (codigo == null || codigo.isUtilizado() || !codigo.getExpiraEm().isAfter(Instant.now())
                || codigo.getTentativas() >= 5) throw new EmailNaoConfirmadoException();
        codigo.setTentativas(codigo.getTentativas() + 1);
        if (!MessageDigest.isEqual(codigo.getCodigoHash().getBytes(StandardCharsets.US_ASCII),
                hash(email + ":" + valor).getBytes(StandardCharsets.US_ASCII))) throw new EmailNaoConfirmadoException();
        codigo.setUtilizado(true);
    }
    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    @Transactional
    public void limparExpirados() { codigos.removerExpirados(Instant.now().minusSeconds(86400)); }
    private String normalizar(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String hash(String valor) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("confirmacao-email:" + valor).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException("Falha na proteção do código."); }
    }
}
