package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.DTO.CadastroProfissionalDTO;
import br.com.neurohelp.tcc_backend.DTO.CadastroResponsavelDTO;
import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.responsavelRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

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
        String email = normalizarEmail(dto.email());
        if (emailEmUso(email)) {
            return ResponseEntity.status(409).body("Não foi possível concluir o cadastro com os dados informados.");
        }

        UserProf usuario = new UserProf();
        usuario.setNome(normalizarTexto(dto.nome()));
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setCpf(somenteDigitos(dto.cpf()));
        usuario.setTelefone(normalizarTelefone(dto.telefone()));
        usuario.setEstado(normalizarTextoOpcional(dto.estado()));
        usuario.setCidade(normalizarTextoOpcional(dto.cidade()));
        usuario.setFormacao(normalizarTextoOpcional(dto.formacao()));
        usuario.setBio(normalizarTextoOpcional(dto.bio()));
        usuario.setNumRegistro(normalizarTextoOpcional(dto.numRegistro()));

        profissionalRepository.save(usuario);
        return ResponseEntity.ok("Profissional cadastrado com sucesso");
    }

    @PostMapping("/responsavel")
    public ResponseEntity<?> cadastrarResponsavel(@Valid @RequestBody CadastroResponsavelDTO dto) {
        String email = normalizarEmail(dto.email());
        if (emailEmUso(email)) {
            return ResponseEntity.status(409).body("Não foi possível concluir o cadastro com os dados informados.");
        }

        UserResp usuario = new UserResp();
        usuario.setNome(normalizarTexto(dto.nome()));
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setCpf(normalizarCpfOpcional(dto.cpf()));
        usuario.setTelefone(normalizarTelefone(dto.telefone()));
        usuario.setEstado(normalizarTextoOpcional(dto.estado()));
        usuario.setCidade(normalizarTextoOpcional(dto.cidade()));

        responsavelRepository.save(usuario);
        return ResponseEntity.ok("Responsável cadastrado com sucesso");
    }

    private boolean emailEmUso(String email) {
        return profissionalRepository.findByEmail(email).isPresent()
                || responsavelRepository.findByEmail(email).isPresent();
    }

    private String normalizarEmail(String valor) {
        return valor.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarTexto(String valor) {
        return valor.trim();
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }

    private String somenteDigitos(String valor) {
        return valor.replaceAll("\\D", "");
    }

    private String normalizarCpfOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return somenteDigitos(valor);
    }

    private String normalizarTelefone(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return somenteDigitos(valor);
    }
}
