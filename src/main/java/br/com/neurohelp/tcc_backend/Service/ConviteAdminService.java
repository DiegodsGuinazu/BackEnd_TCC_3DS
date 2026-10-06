package br.com.neurohelp.tcc_backend.Service;

import br.com.neurohelp.tcc_backend.DTO.CadastroAdminDTO;
import br.com.neurohelp.tcc_backend.Entity.User.*;
import br.com.neurohelp.tcc_backend.Repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class ConviteAdminService {
    private final ConviteAdminRepository convites;
    private final AdminRepository admins;
    private final profissionalRepository profissionais;
    private final responsavelRepository responsaveis;
    private final PasswordEncoder encoder;
    private final String frontendUrl;
    private final Duration validade;
    private final SecureRandom random = new SecureRandom();

    public record ConviteResumo(Long id, String email, Instant expiraEm, Instant utilizadoEm, Instant revogadoEm) {}
    // O link é devolvido uma única vez ao emissor; nunca é persistido ou registrado em log.
    public record ConviteGerado(Long id, String link, Instant expiraEm) {}
    public record ConviteValidado(String email, Instant expiraEm) {}

    public ConviteAdminService(ConviteAdminRepository convites, AdminRepository admins,
                              profissionalRepository profissionais, responsavelRepository responsaveis,
                              PasswordEncoder encoder,
                              @Value("${app.admin.frontend-url}") String frontendUrl,
                              @Value("${app.admin.invite-ttl:PT24H}") Duration validade) {
        URI uri = URI.create(frontendUrl);
        if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null
                || uri.getQuery() != null || uri.getFragment() != null || uri.getUserInfo() != null
                || validade.isNegative() || validade.isZero() || validade.compareTo(Duration.ofDays(7)) > 0) {
            throw new IllegalArgumentException("Configure a URL do frontend e validade de convite entre 1 segundo e 7 dias.");
        }
        this.convites = convites; this.admins = admins; this.profissionais = profissionais;
        this.responsaveis = responsaveis; this.encoder = encoder;
        this.frontendUrl = frontendUrl.replaceAll("/+$", ""); this.validade = validade;
    }

    public ConviteGerado gerar(String email) {
        email = email.trim().toLowerCase(Locale.ROOT);
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 254) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um e-mail válido.");
        }
        verificarEmailLivre(email);
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ConviteAdmin convite = new ConviteAdmin();
        convite.setEmail(email); convite.setTokenHash(hash(token));
        convite.setExpiraEm(Instant.now().plus(validade)); convites.saveAndFlush(convite);
        // Fragmento evita enviar o segredo a logs HTTP de acesso e cabeçalho Referer do site.
        return new ConviteGerado(convite.getId(), frontendUrl + "/pages/cadastro-admin.html#token=" + token, convite.getExpiraEm());
    }

    public ConviteGerado gerarPrimeiro(String email) {
        if (admins.count() != 0) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Já existe administrador. Gere convites pela área autenticada.");
        return gerar(email);
    }

    @Transactional(readOnly = true)
    public ConviteValidado validar(String token) {
        ConviteAdmin convite = ativo(convites.findByTokenHash(hash(token)).orElseThrow(this::invalido));
        return new ConviteValidado(convite.getEmail(), convite.getExpiraEm());
    }

    public void cadastrar(CadastroAdminDTO dto) {
        // A mesma linha fica bloqueada até o commit ou rollback do cadastro completo.
        ConviteAdmin convite = ativo(convites.bloquearPorHash(hash(dto.token())).orElseThrow(this::invalido));
        String email = dto.email().trim().toLowerCase(Locale.ROOT);
        if (!convite.getEmail().equals(email)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Use o e-mail autorizado neste convite.");
        verificarEmailLivre(email);
        if (dto.senha().getBytes(StandardCharsets.UTF_8).length > 72) throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "A senha deve ocupar no máximo 72 bytes.");
        UserAdmin admin = new UserAdmin(); admin.setNome(dto.nome().trim()); admin.setEmail(email);
        admin.setSenha(encoder.encode(dto.senha())); admins.saveAndFlush(admin);
        convite.setUtilizadoEm(Instant.now()); convites.saveAndFlush(convite);
    }

    @Transactional(readOnly = true)
    public List<ConviteResumo> listar() {
        return convites.findAllByOrderByIdDesc().stream().map(c -> new ConviteResumo(
                c.getId(), c.getEmail(), c.getExpiraEm(), c.getUtilizadoEm(), c.getRevogadoEm())).toList();
    }

    public void revogar(Long id) {
        ConviteAdmin convite = convites.bloquearPorId(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Convite não encontrado."));
        if (convite.getUtilizadoEm() != null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "O convite já foi utilizado.");
        if (convite.getRevogadoEm() == null) convite.setRevogadoEm(Instant.now());
    }

    private void verificarEmailLivre(String email) {
        if (admins.findByEmailIgnoreCase(email).isPresent() || profissionais.findByEmailIgnoreCase(email).isPresent()
                || responsaveis.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não foi possível concluir com o e-mail informado.");
        }
    }

    private ConviteAdmin ativo(ConviteAdmin c) {
        if (c.getRevogadoEm() != null || c.getUtilizadoEm() != null || !c.getExpiraEm().isAfter(Instant.now())) {
            throw invalido();
        }
        return c;
    }
    private ResponseStatusException invalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Convite inválido, expirado, revogado ou já utilizado. Solicite um novo convite.");
    }
    private String hash(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalido();
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.US_ASCII))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 indisponível."); }
    }
}
