package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.DTO.Login;
import br.com.neurohelp.tcc_backend.Entity.User.UsuarioAutenticavel;
import br.com.neurohelp.tcc_backend.Repository.ProfissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.ResponsavelRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import br.com.neurohelp.tcc_backend.Security.TokenService;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final ProfissionalRepository profissionalRepository;
    private final ResponsavelRepository responsavelRepository;
    private final TokenService tokenService;

    public AuthController(ProfissionalRepository profissionalRepository, ResponsavelRepository responsavelRepository, TokenService tokenService) {
        this.profissionalRepository = profissionalRepository;
        this.responsavelRepository = responsavelRepository;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login dados) {

        UsuarioAutenticavel usuario = profissionalRepository.findByEmail(dados.getEmail())
                .map(u -> (UsuarioAutenticavel) u)
                .orElse(null);

        if (usuario == null) {
            usuario = responsavelRepository.findByEmail(dados.getEmail())
                    .map(u -> (UsuarioAutenticavel) u)
                    .orElse(null);
        }

        if (usuario == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Email ou senha inválidos.");
        }

        if (!usuario.getSenha().equals(dados.getSenha())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Email ou senha inválidos.");
        }

        String token = tokenService.gerarToken(usuario);

        Map<String, String> resposta = new HashMap<>();
        resposta.put("token", token);
        resposta.put("email", usuario.getEmail());

        return ResponseEntity.ok(resposta);
    }

}