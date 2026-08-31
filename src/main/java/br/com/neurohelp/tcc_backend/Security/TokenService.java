package br.com.neurohelp.tcc_backend.Security;

import br.com.neurohelp.tcc_backend.Entity.User.UsuarioAutenticavel;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${api.security.token.secret:neurohelp-chave-secreta-super-segura-e-longa-123456}")
    private String secretKey;

    // Gera o token na autenticação
    public String gerarToken(UsuarioAutenticavel usuario) {
        Algorithm algoritmo = Algorithm.HMAC256(secretKey);

        return JWT.create()
                .withIssuer("API Neurohelp")
                .withSubject(usuario.getEmail())
                .withExpiresAt(gerarDataExpiracao())
                .sign(algoritmo);
    }

    // Valida o token recebido no filtro de segurança
    public String validarToken(String tokenJWT) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(secretKey);
            return JWT.require(algoritmo)
                    .withIssuer("API Neurohelp")
                    .build()
                    .verify(tokenJWT)
                    .getSubject();
            // Retorna o e-mail do usuário se o token for válido
        } catch (JWTVerificationException exception) {
            return "";
            // Retorna string vazia se o token for inválido ou expirado
        }
    }

    private Instant gerarDataExpiracao() {
        // Validade de 2 horas
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
