package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Repository.ProfissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.ResponsavelRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cadastro")
@CrossOrigin(origins = "*")
public class CadastroController {

    private final ProfissionalRepository profissionalRepository;
    private final ResponsavelRepository responsavelRepository;

    public CadastroController(
            ProfissionalRepository profissionalRepository,
            ResponsavelRepository responsavelRepository) {

        this.profissionalRepository = profissionalRepository;
        this.responsavelRepository = responsavelRepository;
    }

    @PostMapping("/profissional")
    public ResponseEntity<?> cadastrarProfissional(
            @RequestBody UserProf usuario) {

        profissionalRepository.save(usuario);

        return ResponseEntity.ok("Profissional cadastrado com sucesso");
    }

    @PostMapping("/responsavel")
    public ResponseEntity<?> cadastrarResponsavel(
            @RequestBody UserResp usuario) {

        responsavelRepository.save(usuario);

        return ResponseEntity.ok("Responsável cadastrado com sucesso");
    }
}