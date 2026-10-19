package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Validation.Cpf;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CpfController {
    public record Entrada(@Size(max = 32) String cpf) {}
    public record Resultado(boolean valido, String tipoValidacao, String mensagem) {}
    @PostMapping("/api/validacoes/cpf")
    public ResponseEntity<Resultado> validar(@Valid @RequestBody Entrada entrada) {
        boolean valido = Cpf.valido(entrada.cpf());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Resultado(valido,
                "DIGITOS_VERIFICADORES", valido ? "Dígitos verificadores válidos. Existência e titularidade não verificadas."
                : "CPF inválido. Confira os números informados."));
    }
}
