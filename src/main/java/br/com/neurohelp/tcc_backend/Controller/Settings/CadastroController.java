package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.responsavelRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cadastro")
@CrossOrigin(origins = "*")
public class CadastroController {

    private final profissionalRepository profissionalRepository;
    private final responsavelRepository responsavelRepository;
    private final PasswordEncoder passwordEncoder;

    public CadastroController(
            profissionalRepository profissionalRepository,
            responsavelRepository responsavelRepository,
            PasswordEncoder passwordEncoder) {

        this.profissionalRepository = profissionalRepository;
        this.responsavelRepository = responsavelRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/profissional")
    public ResponseEntity<?> cadastrarProfissional(@RequestBody UserProf usuario) {
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        profissionalRepository.save(usuario);
        return ResponseEntity.ok("Profissional cadastrado com sucesso");
    }

    @PostMapping("/responsavel")
    public ResponseEntity<?> cadastrarResponsavel(@RequestBody UserResp usuario) {
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        responsavelRepository.save(usuario);
        return ResponseEntity.ok("Responsável cadastrado com sucesso");
    }
}