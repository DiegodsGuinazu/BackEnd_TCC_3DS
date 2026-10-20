package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.Service.EmailNaoConfirmadoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {CadastroController.class, ConfirmacaoEmailController.class})
public class ConfirmacaoEmailExceptionHandler {
    @ExceptionHandler(EmailNaoConfirmadoException.class)
    public ResponseEntity<?> naoConfirmado() {
        return ResponseEntity.status(422).cacheControl(CacheControl.noStore()).body(Map.of(
                "codigo", "EMAIL_NAO_CONFIRMADO", "mensagem", EmailNaoConfirmadoException.MENSAGEM));
    }
    @RestControllerAdvice(assignableTypes = ConfirmacaoEmailController.class)
    public static class Concorrencia {
        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<?> ocupado() {
            return ResponseEntity.status(409).cacheControl(CacheControl.noStore()).body(Map.of(
                    "mensagem", "Uma solicitação já está em processamento. Aguarde um minuto e tente novamente."));
        }
    }
}
