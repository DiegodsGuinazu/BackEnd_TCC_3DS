package br.com.neurohelp.tcc_backend.config;

import br.com.neurohelp.tcc_backend.Security.SecurityFilter;
import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("!bootstrap-convite")
public class SecurityConfig {

    private final SecurityFilter securityFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Libera todas as requisições OPTIONS do navegador (CORS Preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login",
                                "/cadastro/profissional", "/cadastro/responsavel",
                                "/cadastro/admin", "/cadastro/admin/convite/validar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/profissionais/*/foto").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/aprendizagem", "/api/aprendizagem/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/aprendizagem", "/api/aprendizagem/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/aprendizagem", "/api/aprendizagem/**").hasRole("ADMIN")
                        .requestMatchers("/error").permitAll()
                        // Somente as listagens GET são públicas; detalhes e escrita continuam protegidos.
                        .requestMatchers(HttpMethod.GET, "/api/profissionais", "/api/aprendizagem").permitAll()
                        // O tipo vem do usuário carregado do banco, nunca do cliente.
                        .requestMatchers("/api/perfil", "/api/perfil/**")
                        .access((authentication, context) -> new AuthorizationDecision(
                                authentication.get().getPrincipal() instanceof UserProf))
                        .requestMatchers("/api/perfil-responsavel", "/api/perfil-responsavel/**")
                        .access((authentication, context) -> new AuthorizationDecision(
                                authentication.get().getPrincipal() instanceof UserResp))
                        .requestMatchers("/api/conta/foto", "/usuarios/**")
                        .access((authentication, context) -> new AuthorizationDecision(
                                authentication.get().getPrincipal() instanceof UserResp
                                || authentication.get().getPrincipal() instanceof UserProf))
                        // A publicação aguarda um perfil de equipe/administrador.
                        .requestMatchers("/publicacao/**", "/h2-console/**").denyAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\":401,\"mensagem\":\"Autenticação necessária ou token inválido.\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\":403,\"mensagem\":\"Acesso não permitido.\"}");
                        }))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .formLogin(form -> form.disable())
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
