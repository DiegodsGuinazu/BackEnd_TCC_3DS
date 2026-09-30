package br.com.neurohelp.tcc_backend.Security;

import br.com.neurohelp.tcc_backend.Entity.User.UsuarioAutenticavel;
import com.auth0.jwt.interfaces.DecodedJWT;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.responsavelRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private profissionalRepository profissionalRepository;

    @Autowired
    private responsavelRepository responsavelRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        return "OPTIONS".equalsIgnoreCase(method)
                || ("POST".equalsIgnoreCase(method) && (
                        path.equals("/auth/login")
                        || path.equals("/cadastro/profissional")
                        || path.equals("/cadastro/responsavel")))
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        var tokenJWT = recuperarToken(request);

        if (tokenJWT != null) {
            var token = tokenService.validarTokenDecodificado(tokenJWT);
            if (token.isPresent()) {
                UsuarioAutenticavel usuario = recuperarUsuario(token.get());
                if (usuario != null) {
                    var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private UsuarioAutenticavel recuperarUsuario(DecodedJWT token) {
        String email = token.getSubject();
        if (email == null || email.isBlank()) return null;
        String tipoPerfil = token.getClaim("tipoPerfil").asString();
        if ("PROFISSIONAL".equals(tipoPerfil)) return profissionalRepository.findByEmail(email).orElse(null);
        if ("RESPONSAVEL".equals(tipoPerfil)) return responsavelRepository.findByEmail(email).orElse(null);
        if (!token.getClaim("tipoPerfil").isMissing()) return null;

        // Compatibilidade com JWTs anteriores: somente uma identidade sem ambiguidade.
        var profissional = profissionalRepository.findByEmail(email).orElse(null);
        var responsavel = responsavelRepository.findByEmail(email).orElse(null);
        if (profissional != null && responsavel != null) return null;
        return profissional != null ? profissional : responsavel;
    }

    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring("Bearer ".length()).trim();
            if (!token.isEmpty() && !"null".equalsIgnoreCase(token) && !"undefined".equalsIgnoreCase(token)) {
                return token;
            }
        }
        return null;
    }
}
