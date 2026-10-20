package br.com.neurohelp.tcc_backend.Service;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public class EmailNaoConfirmadoException extends ResponseStatusException {
    public static final String MENSAGEM = "E-mail não confirmado. Código inválido, expirado ou já utilizado. Confira o código ou solicite outro.";
    public EmailNaoConfirmadoException() { super(HttpStatusCode.valueOf(422), MENSAGEM); }
}
