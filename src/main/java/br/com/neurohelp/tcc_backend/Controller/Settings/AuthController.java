package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.DTO.Login;
import br.com.neurohelp.tcc_backend.Entity.User.UsuarioAutenticavel;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.responsavelRepository;
import br.com.neurohelp.tcc_backend.Repository.AdminRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import br.com.neurohelp.tcc_backend.Security.TokenService;
import java.util.HashMap;
import java.util.Map;

    @RestController
    @RequestMapping("/auth")
    public class AuthController {

        private final profissionalRepository profissionalRepository;
        private final responsavelRepository responsavelRepository;
        private final TokenService tokenService;
        private final PasswordEncoder passwordEncoder;
        private final AdminRepository admins;

        public AuthController(profissionalRepository profissionalRepository, responsavelRepository responsavelRepository, TokenService tokenService, PasswordEncoder passwordEncoder, AdminRepository admins) {
            this.admins = admins;
            this.profissionalRepository = profissionalRepository;
            this.responsavelRepository = responsavelRepository;
            this.tokenService = tokenService;
            this.passwordEncoder = passwordEncoder;
        }

        @PostMapping("/login")
        public ResponseEntity<?> login(@RequestBody Login dados) {

            if (dados.getEmail() == null || dados.getSenha() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou senha inválidos.");
            }
            String email = dados.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
            var profissional = profissionalRepository.findByEmailIgnoreCase(email).orElse(null);
            var responsavel = responsavelRepository.findByEmailIgnoreCase(email).orElse(null);
            var admin = admins.findByEmailIgnoreCase(email).orElse(null);
            if ((profissional != null ? 1 : 0) + (responsavel != null ? 1 : 0) + (admin != null ? 1 : 0) > 1) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou senha inválidos.");
            }
            UsuarioAutenticavel usuario = admin != null ? admin : profissional != null ? profissional : responsavel;

            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou senha inválidos.");
            }

            if (!passwordEncoder.matches(dados.getSenha(), usuario.getSenha())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou senha inválidos.");
            }

            String token = tokenService.gerarToken(usuario);

            Map<String, String> resposta = new HashMap<>();
            resposta.put("token", token);
            resposta.put("email", usuario.getEmail());

            return ResponseEntity.ok(resposta);
        }
    }
