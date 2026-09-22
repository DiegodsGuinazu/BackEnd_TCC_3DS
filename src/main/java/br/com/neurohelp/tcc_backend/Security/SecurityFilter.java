package br.com.neurohelp.tcc_backend.Security;

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
import org.springframework.security.core.userdetails.UserDetails;
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
                || path.equals("/auth/login")
                || path.equals("/login")
                || path.startsWith("/cadastro")
                || path.startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        var tokenJWT = recuperarToken(request);

        if (tokenJWT != null) {
            try {
                var subject = tokenService.validarToken(tokenJWT);
                if (subject != null && !subject.trim().isEmpty()) {

                    UserDetails usuario = (UserDetails) profissionalRepository.findByEmail(subject).orElse(null);

                    if (usuario == null) {
                        usuario = (UserDetails) responsavelRepository.findByEmail(subject).orElse(null);
                    }

                    if (usuario != null) {
                        var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                // Em rotas protegidas que falharem o token, apenas limpa a autenticação
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.replace("Bearer ", "").trim();
            // Valida se o token retornado não é uma string vazia ou "null"/"undefined"
            if (!token.isEmpty() && !"null".equalsIgnoreCase(token) && !"undefined".equalsIgnoreCase(token)) {
                return token;
            }
        }
        return null;
    }
}