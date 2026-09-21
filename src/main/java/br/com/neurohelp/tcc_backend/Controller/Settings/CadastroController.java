package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.responsavelRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cadastro")
@CrossOrigin(origins = "*")
public class CadastroController {

    private final profissionalRepository profissionalRepository;
    private final responsavelRepository responsavelRepository;

    public CadastroController(
            profissionalRepository profissionalRepository,
            responsavelRepository responsavelRepository) {

        this.profissionalRepository = profissionalRepository;
        this.responsavelRepository = responsavelRepository;
    }

    @PostMapping("/profissional")
    public ResponseEntity<?> cadastrarProfissional(
            @RequestBody UserProf usuario) {

        System.out.println("Cadastrando profissional");
        System.out.println(usuario.getNome());

        profissionalRepository.save(usuario);

        return ResponseEntity.ok("Profissional cadastrado com sucesso");
    }

    @PostMapping("/responsavel")
    public ResponseEntity<?> cadastrarResponsavel(
            @RequestBody UserResp usuario) {

        System.out.println("Cadastrando responsavel");
        System.out.println(usuario.getNome());

        responsavelRepository.save(usuario);

        return ResponseEntity.ok("Responsável cadastrado com sucesso");
    }
}