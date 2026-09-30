package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.DTO.CadastroProfissionalDTO;
import br.com.neurohelp.tcc_backend.DTO.CadastroResponsavelDTO;
import jakarta.validation.Valid;
import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.responsavelRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cadastro")
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
    public ResponseEntity<?> cadastrarProfissional(@Valid @RequestBody CadastroProfissionalDTO dto) {
        if (emailEmUso(dto.email())) return ResponseEntity.status(409).body("Não foi possível concluir o cadastro com os dados informados.");
        UserProf usuario = new UserProf();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setCpf(dto.cpf());
        usuario.setTelefone(dto.telefone());
        usuario.setEstado(dto.estado());
        usuario.setBio(dto.bio());
        usuario.setNumRegistro(dto.numRegistro());
        profissionalRepository.save(usuario);
        return ResponseEntity.ok("Profissional cadastrado com sucesso");
    }

    @PostMapping("/responsavel")
    public ResponseEntity<?> cadastrarResponsavel(@Valid @RequestBody CadastroResponsavelDTO dto) {
        if (emailEmUso(dto.email())) return ResponseEntity.status(409).body("Não foi possível concluir o cadastro com os dados informados.");
        UserResp usuario = new UserResp();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setCpf(dto.cpf());
        usuario.setTelefone(dto.telefone());
        usuario.setEstado(dto.estado());
        responsavelRepository.save(usuario);
        return ResponseEntity.ok("Responsável cadastrado com sucesso");
    }

    private boolean emailEmUso(String email) {
        return profissionalRepository.findByEmail(email).isPresent()
                || responsavelRepository.findByEmail(email).isPresent();
    }
}
