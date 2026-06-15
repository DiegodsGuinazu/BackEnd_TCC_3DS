package br.com.neurohelp.tcc_backend.Security;


import br.com.neurohelp.tcc_backend.Entity.User.UsuarioAutenticavel;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.stereotype.Service;




@Service
public class TokenService{
    private static final String SECRET_KEY = "neurohelp-chave-secreta";

    public String gerarToken(UsuarioAutenticavel usuario){
        return JWT.create().withSubject(usuario.getEmail())
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }
}
