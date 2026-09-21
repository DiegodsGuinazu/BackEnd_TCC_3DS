package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.DTO.Login;
import br.com.neurohelp.tcc_backend.Entity.User.UsuarioAutenticavel;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.responsavelRepository;
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

    private final profissionalRepository profissionalRepository;
    private final responsavelRepository responsavelRepository;
    private final TokenService tokenService;

    public AuthController(profissionalRepository profissionalRepository, responsavelRepository responsavelRepository, TokenService tokenService) {
        this.profissionalRepository = profissionalRepository;
        this.responsavelRepository = responsavelRepository;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login dados) {

        var prof = profissionalRepository.findByEmail(dados.getEmail());

        var resp = responsavelRepository.findbyEmail(dados.getEmail());

        UsuarioAutenticavel usuario = (UsuarioAutenticavel) profissionalRepository.findByEmail(dados.getEmail()).orElse(null);

        if (usuario == null) {
            usuario = (UsuarioAutenticavel) responsavelRepository.findbyEmail(dados.getEmail()).orElse(null);
        }

        if (usuario != null) {
            System.out.println("Senha banco: " + usuario.getSenha());
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