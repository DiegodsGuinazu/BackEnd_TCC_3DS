package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.Service.ConfirmacaoEmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class ConfirmacaoEmailController {
    public record Solicitar(@NotBlank @Size(max = 254)
            @Pattern(regexp = "[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?\\.[A-Za-z]{2,}") String email) {
        @Override public String toString() { return "Solicitar[conteúdo protegido]"; }
    }
    private final ConfirmacaoEmailService service;
    public ConfirmacaoEmailController(ConfirmacaoEmailService service) { this.service = service; }
    @PostMapping("/cadastro/email/solicitar")
    public ResponseEntity<?> solicitar(@Valid @RequestBody Solicitar dto, HttpServletRequest request) {
        service.solicitar(dto.email(), request.getRemoteAddr());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("mensagem",
                "Se o endereço puder ser usado em um novo cadastro, você receberá um código. Confira também o spam. Se já possui uma conta, faça login."));
    }
}
