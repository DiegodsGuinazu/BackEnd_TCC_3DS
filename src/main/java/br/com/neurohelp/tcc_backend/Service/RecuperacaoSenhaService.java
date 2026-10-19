package br.com.neurohelp.tcc_backend.Service;

import br.com.neurohelp.tcc_backend.DTO.RecuperacaoSenhaDTO;
import br.com.neurohelp.tcc_backend.Entity.User.*;
import br.com.neurohelp.tcc_backend.Repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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

@Service
public class RecuperacaoSenhaService {
    private final profissionalRepository profissionais;
    private final responsavelRepository responsaveis;
    private final AdminRepository admins;
    private final RecuperacaoSenhaRepository codigos;
    private final EntityManager em;
    private final PasswordEncoder encoder;
    private final EmailRecuperacaoService emails;
    private final ApplicationEventPublisher eventos;
    private final RecuperacaoLimites limites;
    private final String segredo;
    private final SecureRandom random = new SecureRandom();
    public RecuperacaoSenhaService(profissionalRepository profissionais, responsavelRepository responsaveis,
            AdminRepository admins, RecuperacaoSenhaRepository codigos, EntityManager em, PasswordEncoder encoder,
            EmailRecuperacaoService emails, ApplicationEventPublisher eventos, RecuperacaoLimites limites,
            @Value("${api.security.token.secret}") String segredo) {
        this.profissionais = profissionais; this.responsaveis = responsaveis; this.admins = admins;
        this.codigos = codigos; this.em = em; this.encoder = encoder; this.emails = emails;
        this.eventos = eventos; this.limites = limites; this.segredo = segredo;
    }
    @Transactional
    public void solicitar(String emailInformado, String origem) {
        // A disponibilidade não depende da existência da conta.
        if (!emails.configurado()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Recuperação de senha indisponível. Tente novamente mais tarde.");
        String email = normalizar(emailInformado);
        limites.verificar("envio-ip:" + hash(origem), 20);
        limites.verificar("envio-email:" + hash(email), 3);
        UsuarioAutenticavel usuario = carregarComBloqueio(email);
        if (usuario == null) return;
        String chave = hash(email);
        Instant agora = Instant.now();
        RecuperacaoSenha codigo = codigos.findById(chave).orElseGet(RecuperacaoSenha::new);
        if (codigo.getEnviadoEm() != null && codigo.getEnviadoEm().plusSeconds(60).isAfter(agora)) return;
        String valor = String.format(Locale.ROOT, "%08d", random.nextInt(100_000_000));
        codigo.setEmailHash(chave); codigo.setCodigoHash(hash(email + ":" + valor));
        codigo.setExpiraEm(agora.plusSeconds(900)); codigo.setEnviadoEm(agora);
        codigo.setTentativas(0); codigo.setUtilizado(false);
        codigo.setVersaoCredenciais(usuario.getVersaoCredenciais());
        codigos.save(codigo);
        // O destinatário é sempre o endereço da conta carregada do banco.
        eventos.publishEvent(new RecuperacaoEmailListener.Envio(usuario.getEmail(), valor));
    }
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void redefinir(RecuperacaoSenhaDTO.Redefinir dto, String origem) {
        String email = normalizar(dto.email());
        limites.verificar("confirma-ip:" + hash(origem), 30);
        limites.verificar("confirma-email:" + hash(email), 15);
        UsuarioAutenticavel usuario = carregarComBloqueio(email);
        RecuperacaoSenha codigo = usuario == null ? null : codigos.findById(hash(email)).orElse(null);
        if (codigo == null || codigo.isUtilizado() || !codigo.getExpiraEm().isAfter(Instant.now())
                || codigo.getTentativas() >= 5 || codigo.getVersaoCredenciais() != usuario.getVersaoCredenciais()) throw invalido();
        codigo.setTentativas(codigo.getTentativas() + 1);
        if (!MessageDigest.isEqual(codigo.getCodigoHash().getBytes(StandardCharsets.US_ASCII),
                hash(email + ":" + dto.codigo()).getBytes(StandardCharsets.US_ASCII))) throw invalido();
        if (dto.novaSenha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ocupar no máximo 72 bytes.");
        }
        String senha = encoder.encode(dto.novaSenha());
        if (usuario instanceof UserProf u) { u.setSenha(senha); u.setVersaoCredenciais(u.getVersaoCredenciais() + 1); }
        else if (usuario instanceof UserResp u) { u.setSenha(senha); u.setVersaoCredenciais(u.getVersaoCredenciais() + 1); }
        else if (usuario instanceof UserAdmin u) { u.setSenha(senha); u.setVersaoCredenciais(u.getVersaoCredenciais() + 1); }
        codigo.setUtilizado(true);
    }
    private UsuarioAutenticavel carregarComBloqueio(String email) {
        var prof = profissionais.findByEmailIgnoreCase(email).orElse(null);
        var resp = responsaveis.findByEmailIgnoreCase(email).orElse(null);
        var admin = admins.findByEmailIgnoreCase(email).orElse(null);
        if ((prof == null ? 0 : 1) + (resp == null ? 0 : 1) + (admin == null ? 0 : 1) != 1) return null;
        UsuarioAutenticavel usuario = prof != null ? prof : resp != null ? resp : admin;
        // Serializa reenvio e consumo por conta, inclusive a primeira solicitação.
        em.refresh(usuario, LockModeType.PESSIMISTIC_WRITE);
        return usuario;
    }
    private String normalizar(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String hash(String valor) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("recuperacao:" + valor).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException("Falha na proteção do código."); }
    }
    private ResponseStatusException invalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido, expirado ou já utilizado. Solicite um novo código.");
    }
}
