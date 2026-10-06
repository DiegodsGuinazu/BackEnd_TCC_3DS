package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.DTO.*;
import br.com.neurohelp.tcc_backend.Service.ConviteAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
public class ConviteAdminController {
    private final ConviteAdminService service;
    public ConviteAdminController(ConviteAdminService service) { this.service = service; }
    public record TokenDTO(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token) {}

    @PostMapping("/cadastro/admin/convite/validar")
    public ResponseEntity<?> validar(@Valid @RequestBody TokenDTO dto) {
        return semCache(service.validar(dto.token()));
    }
    @PostMapping("/cadastro/admin")
    public ResponseEntity<?> cadastrar(@Valid @RequestBody CadastroAdminDTO dto) {
        service.cadastrar(dto);
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(Map.of("mensagem", "Administrador cadastrado com sucesso. Entre com seu e-mail e senha."));
    }
    @PostMapping("/api/admin/convites") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> gerar(@Valid @RequestBody ConviteEmailDTO dto) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(service.gerar(dto.email()));
    }
    @GetMapping("/api/admin/convites") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ConviteAdminService.ConviteResumo>> listar() { return semCache(service.listar()); }
    @DeleteMapping("/api/admin/convites/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revogar(@PathVariable Long id) { service.revogar(id); return ResponseEntity.noContent().build(); }
    private <T> ResponseEntity<T> semCache(T body) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body); }
}
