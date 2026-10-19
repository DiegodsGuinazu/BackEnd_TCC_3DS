package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.DTO.RecuperacaoSenhaDTO;
import br.com.neurohelp.tcc_backend.Entity.User.UsuarioAutenticavel;
import br.com.neurohelp.tcc_backend.Service.RecuperacaoSenhaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class RecuperacaoSenhaController {
    public static final String SOLICITADO = "Se o e-mail estiver cadastrado, você receberá um código de recuperação. Verifique também a pasta de spam.";
    private final RecuperacaoSenhaService service;
    public RecuperacaoSenhaController(RecuperacaoSenhaService service) { this.service = service; }
    @PostMapping("/auth/esqueci-senha")
    public ResponseEntity<?> solicitar(@Valid @RequestBody RecuperacaoSenhaDTO.Solicitar dto, HttpServletRequest request) {
        service.solicitar(dto.email(), request.getRemoteAddr());
        return resposta(SOLICITADO);
    }
    @PostMapping("/api/conta/recuperacao-senha")
    public ResponseEntity<?> perfil(@AuthenticationPrincipal UsuarioAutenticavel usuario, HttpServletRequest request) {
        service.solicitar(usuario.getEmail(), request.getRemoteAddr());
        return resposta(SOLICITADO);
    }
    @PostMapping("/auth/redefinir-senha")
    public ResponseEntity<?> redefinir(@Valid @RequestBody RecuperacaoSenhaDTO.Redefinir dto, HttpServletRequest request) {
        service.redefinir(dto, request.getRemoteAddr());
        return resposta("Senha alterada. Entre novamente com a nova senha.");
    }
    private ResponseEntity<?> resposta(String mensagem) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("mensagem", mensagem));
    }
}
