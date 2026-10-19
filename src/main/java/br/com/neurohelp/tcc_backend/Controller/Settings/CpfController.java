package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.Service.CpfValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/validacoes/cpf")
public class CpfController {
    public record CpfRequest(@NotBlank @Size(max = 32) String cpf) {}
    public record CpfResponse(boolean valido, boolean cadastroConsultado, String mensagem) {}
    @PostMapping
    public ResponseEntity<CpfResponse> validar(@Valid @RequestBody CpfRequest dto) {
        boolean valido = CpfValidator.valido(dto.cpf());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new CpfResponse(valido, false,
                valido ? "Dígitos verificadores válidos. Existência e titularidade não verificadas."
                        : "CPF inválido. Confira o número informado."));
    }
}
