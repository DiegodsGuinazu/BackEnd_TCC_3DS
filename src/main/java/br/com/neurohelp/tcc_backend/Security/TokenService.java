package br.com.neurohelp.tcc_backend.Security;


import br.com.neurohelp.tcc_backend.Entity.User.UsuarioAutenticavel;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;




@Service
public class TokenService{
    private static final String SECRET_KEY = "neurohelp-chave-secreta";

    public String gerarToken(UsuarioAutenticavel usuario){
        return JWT.create().withSubject(usuario.getEmail())
                .sign(Pbkdf2PasswordEncoder.Algorithm.HMAC256(SECRET_KEY));
    }
}
