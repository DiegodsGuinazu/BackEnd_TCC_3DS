package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Controller.Perfil.FotoPerfilController;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {ConviteAdminController.class, AprendizagemController.class,
        FotoPerfilController.class})
public class IntegracaoExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> esperado(ResponseStatusException e) {
        return resposta(e.getStatusCode(), e.getReason() == null ? "Não foi possível concluir a operação." : e.getReason());
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<?> validacao(Exception e) {
        // Não devolve valores rejeitados, payload, senha ou token de convite.
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos. Verifique os campos e os requisitos informados.");
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflito() {
        return resposta(HttpStatus.CONFLICT, "Não foi possível concluir a operação com os dados informados.");
    }
    private ResponseEntity<?> resposta(HttpStatusCode status, String mensagem) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore())
                .body(Map.of("status", status.value(), "mensagem", mensagem));
    }
}
