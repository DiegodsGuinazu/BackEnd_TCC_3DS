package br.com.neurohelp.tcc_backend.Service;

import br.com.neurohelp.tcc_backend.Entity.User.*;
import br.com.neurohelp.tcc_backend.Repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class RecuperacaoSenhaService {
    private final RecuperacaoSenhaRepository recuperacoes;
    private final profissionalRepository profissionais;
    private final responsavelRepository responsaveis;
    private final PasswordEncoder encoder;
    private final EmailRecuperacaoService emailService;
    private final String secret;
    private final EnvioRecuperacaoService envio;
    private final SecureRandom random = new SecureRandom();
    public RecuperacaoSenhaService(RecuperacaoSenhaRepository recuperacoes, profissionalRepository profissionais,
            responsavelRepository responsaveis, PasswordEncoder encoder, EmailRecuperacaoService emailService, EnvioRecuperacaoService envio,
            @Value("${api.security.token.secret}") String secret) {
        this.recuperacoes = recuperacoes; this.profissionais = profissionais; this.responsaveis = responsaveis;
        this.encoder = encoder; this.emailService = emailService; this.secret = secret; this.envio = envio;
    }
    @Transactional
    public void solicitar(String entrada) {
        if (!emailService.configurado()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Recuperação de senha indisponível no momento. Tente mais tarde.");
        String email = normalizar(entrada);
        var profissional = profissionais.buscarParaRecuperacao(email).orElse(null);
        var responsavel = responsaveis.buscarParaRecuperacao(email).orElse(null);
        // Não escolhe uma identidade arbitrária em cadastros legados ambíguos.
        if ((profissional == null) == (responsavel == null)) return;
        UsuarioAutenticavel usuario = profissional != null ? profissional : responsavel;
        var registro = recuperacoes.buscarComBloqueio(email).orElse(null);
        Instant agora = Instant.now();
        // Mesmo retorno para conta inexistente, cooldown, código ativo e falha de entrega.
        // Não permite renovar o orçamento de tentativas enquanto o código estiver ativo.
        if (registro != null && registro.getSolicitadoEm().plus(Duration.ofMinutes(15)).isAfter(agora)) return;
        if (registro == null) { registro = new RecuperacaoSenha(); registro.setEmail(email); }
        String codigo;
        do { codigo = String.format(Locale.ROOT, "%08d", random.nextInt(100_000_000)); }
        while (hash(email, codigo).equals(registro.getCodigoHash()));
        registro.setCodigoHash(hash(email, codigo));
        registro.setSolicitadoEm(agora); registro.setExpiraEm(agora.plus(Duration.ofMinutes(15)));
        registro.setTentativas(0); registro.setUsado(false); registro.setTokenVersion(usuario.getTokenVersion());
        registro.setTipoPerfil(profissional != null ? "PROFISSIONAL" : "RESPONSAVEL");
        registro.setUsuarioId(profissional != null ? profissional.getId() : responsavel.getId());
        // A unicidade do e-mail impede dois registros concorrentes na primeira solicitação.
        recuperacoes.saveAndFlush(registro);
        envio.agendar(usuario.getEmail(), codigo);
    }
    @Transactional
    public boolean redefinir(String entrada, String codigo, String novaSenha) {
        if (novaSenha.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter no máximo 72 bytes.");
        String email = normalizar(entrada);
        // Mesma ordem de bloqueios usada na solicitação: conta, depois código.
        var profissional = profissionais.buscarParaRecuperacao(email).orElse(null);
        var responsavel = responsaveis.buscarParaRecuperacao(email).orElse(null);
        if ((profissional == null) == (responsavel == null)) return false;
        var registro = recuperacoes.buscarComBloqueio(email).orElse(null);
        if (registro == null || registro.isUsado() || registro.getTentativas() >= 5
                || !registro.getExpiraEm().isAfter(Instant.now())) return false;
        registro.setTentativas(registro.getTentativas() + 1);
        if (!MessageDigest.isEqual(registro.getCodigoHash().getBytes(StandardCharsets.UTF_8),
                hash(email, codigo).getBytes(StandardCharsets.UTF_8))) return false;
        if ("PROFISSIONAL".equals(registro.getTipoPerfil())) {
            var usuario = profissional;
            if (usuario == null || usuario.getId() != registro.getUsuarioId()
                    || usuario.getTokenVersion() != registro.getTokenVersion()) return false;
            usuario.setSenha(encoder.encode(novaSenha)); usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        } else {
            var usuario = responsavel;
            if (usuario == null || usuario.getId() != registro.getUsuarioId()
                    || usuario.getTokenVersion() != registro.getTokenVersion()) return false;
            usuario.setSenha(encoder.encode(novaSenha)); usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        }
        registro.setUsado(true);
        return true;
    }
    private String normalizar(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String hash(String email, String codigo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("recuperacao:" + email + ":" + codigo).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException("Não foi possível proteger o código."); }
    }
}
