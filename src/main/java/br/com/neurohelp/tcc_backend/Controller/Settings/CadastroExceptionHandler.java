package br.com.neurohelp.tcc_backend.Controller.Settings;

import org.springframework.dao.DataIntegrityViolationException;
import br.com.neurohelp.tcc_backend.Service.CrmService.ConsultaException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CadastroController.class)
public class CadastroExceptionHandler {
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> validacao(org.springframework.web.bind.MethodArgumentNotValidException e) {
        var campos = new java.util.TreeMap<String, String>();
        e.getBindingResult().getFieldErrors().forEach(erro -> {
            String mensagem = switch (erro.getCode() == null ? "" : erro.getCode()) {
                case "NotBlank" -> "Preencha este campo.";
                case "Size" -> "O tamanho informado não é permitido.";
                case "Pattern" -> erro.getDefaultMessage();
                default -> "Confira este campo.";
            };
            campos.putIfAbsent(erro.getField(), mensagem);
        });
        return ResponseEntity.badRequest().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(java.util.Map.of("mensagem", "Confira os campos indicados.", "campos", campos));
    }

    @ExceptionHandler(ConsultaException.class)
    public ResponseEntity<String> erroEsperado(ConsultaException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> conflitoDeCadastro() {
        return ResponseEntity.status(409).body("Não foi possível concluir o cadastro com os dados informados.");
    }
}
