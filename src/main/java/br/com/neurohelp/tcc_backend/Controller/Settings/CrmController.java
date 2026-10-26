package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.Service.CrmService;
import br.com.neurohelp.tcc_backend.Service.RecuperacaoLimites;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/validacoes/crm")
public class CrmController {
    private final CrmService crm;
    private final RecuperacaoLimites limites;
    public CrmController(CrmService crm, RecuperacaoLimites limites) { this.crm = crm; this.limites = limites; }
    public record Consulta(@NotBlank @Pattern(regexp = "[0-9]{1,7}") String numero,
                           @NotBlank @Size(min = 2, max = 2) String uf,
                           @NotBlank @Size(max = 120) String nome) {}
    @PostMapping
    public ResponseEntity<CrmService.Resultado> consultar(@Valid @RequestBody Consulta dto, HttpServletRequest request) {
        limites.verificar("crm:" + request.getRemoteAddr(), 20);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(crm.consultar(dto.numero(), dto.uf(), dto.nome()));
    }
}
