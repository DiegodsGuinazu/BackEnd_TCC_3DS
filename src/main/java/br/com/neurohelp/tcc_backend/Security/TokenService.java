package br.com.neurohelp.tcc_backend.Security;

import br.com.neurohelp.tcc_backend.Entity.User.UsuarioAutenticavel;
import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Entity.User.UserAdmin;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.util.Optional;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secretKey;

    public String gerarToken(UsuarioAutenticavel usuario) {
        Algorithm algoritmo = Algorithm.HMAC256(secretKey);
        String tipoPerfil;
        if (usuario instanceof UserProf) tipoPerfil = "PROFISSIONAL";
        else if (usuario instanceof UserResp) tipoPerfil = "RESPONSAVEL";
        else if (usuario instanceof UserAdmin) tipoPerfil = "ADMIN";
        else throw new IllegalArgumentException("Tipo de usuário não suportado.");

        return JWT.create()
                .withIssuer("API EspectroCare")
                .withSubject(usuario.getEmail())
                .withClaim("tipoPerfil", tipoPerfil)
                .withExpiresAt(gerarDataExpiracao())
                .sign(algoritmo);
    }

    public String validarToken(String tokenJWT) {
        return validarTokenDecodificado(tokenJWT).map(DecodedJWT::getSubject).orElse("");
    }

    public Optional<DecodedJWT> validarTokenDecodificado(String tokenJWT) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(secretKey);
            return Optional.of(JWT.require(algoritmo)
                    .withIssuer("API EspectroCare")
                    .build()
                    .verify(tokenJWT));
        } catch (JWTVerificationException exception) {
            return Optional.empty();
        }
    }

    private Instant gerarDataExpiracao() {
        return Instant.now().plus(Duration.ofHours(2));
    }
}
