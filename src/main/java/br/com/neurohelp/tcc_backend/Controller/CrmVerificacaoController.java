package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserAdmin;
import br.com.neurohelp.tcc_backend.Service.CrmVerificacaoService;
import br.com.neurohelp.tcc_backend.Service.RecuperacaoLimites;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class CrmVerificacaoController {
    private final CrmVerificacaoService service;
    private final RecuperacaoLimites limites;
    public CrmVerificacaoController(CrmVerificacaoService service, RecuperacaoLimites limites) {
        this.service = service; this.limites = limites;
    }
    public record Revisao(@NotNull @Positive Long revisao, @NotNull Boolean confirmado) {}
    @PostMapping("/api/perfil/crm/solicitacao")
    public ResponseEntity<?> solicitar(@AuthenticationPrincipal UserProf usuario) {
        limites.verificar("crm:solicitar:" + usuario.getId(), 5);
        return semCache(service.solicitar(usuario.getId()));
    }
    @GetMapping("/api/admin/crm/solicitacoes") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listar(@RequestParam(defaultValue = "0") int pagina) { return semCache(service.listar(pagina)); }
    @PutMapping("/api/admin/crm/solicitacoes/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> revisar(@PathVariable int id, @Valid @RequestBody Revisao dto, @AuthenticationPrincipal UserAdmin admin) {
        return semCache(service.revisar(id, dto.revisao(), dto.confirmado(), admin.getId()));
    }
    private ResponseEntity<?> semCache(Object body) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body); }
}
