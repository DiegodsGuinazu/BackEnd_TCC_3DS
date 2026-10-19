package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Entity.User.*;
import br.com.neurohelp.tcc_backend.Service.RecuperacaoSenhaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
public class RecuperacaoSenhaController {
    public record Solicitacao(@NotBlank @Email @Size(max = 254) String email) {}
    public record Redefinicao(@NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "[0-9]{8}") String codigo,
            @NotBlank @Size(max = 72) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{8,}$") String novaSenha) {}
    private final RecuperacaoSenhaService service;
    public RecuperacaoSenhaController(RecuperacaoSenhaService service) { this.service = service; }
    @PostMapping("/auth/esqueci-senha")
    public ResponseEntity<?> solicitar(@Valid @RequestBody Solicitacao dto) {
        service.solicitar(dto.email()); return aceita();
    }
    @PostMapping("/api/conta/senha/solicitar")
    public ResponseEntity<?> solicitarLogado(@AuthenticationPrincipal UsuarioAutenticavel usuario) {
        if (!(usuario instanceof UserProf || usuario instanceof UserResp))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil não permitido.");
        // O destinatário vem exclusivamente do banco; nenhum e-mail do corpo é aceito.
        service.solicitar(usuario.getEmail()); return aceita();
    }
    @PostMapping("/auth/redefinir-senha")
    public ResponseEntity<?> redefinir(@Valid @RequestBody Redefinicao dto) {
        boolean sucesso = service.redefinir(dto.email(), dto.codigo(), dto.novaSenha());
        return ResponseEntity.status(sucesso ? 200 : 400).cacheControl(CacheControl.noStore()).body(Map.of("mensagem",
                sucesso ? "Senha alterada. Entre novamente em seus dispositivos."
                : "Código inválido, expirado ou sem tentativas disponíveis. Solicite outro após 15 minutos."));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<?> solicitacaoConcorrente() { return aceita(); }

    private ResponseEntity<?> aceita() {
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(Map.of("mensagem",
                "Se o e-mail estiver cadastrado, você receberá um código. Verifique também o spam. Aguarde 15 minutos antes de solicitar outro."));
    }
}
