package br.com.neurohelp.tcc_backend.Controller.Settings;

import org.springframework.dao.DataIntegrityViolationException;
import br.com.neurohelp.tcc_backend.Service.CrmService.ConsultaException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CadastroController.class)
public class CadastroExceptionHandler {
    @ExceptionHandler(ConsultaException.class)
    public ResponseEntity<String> erroEsperado(ConsultaException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> conflitoDeCadastro() {
        return ResponseEntity.status(409).body("Não foi possível concluir o cadastro com os dados informados.");
    }
}
