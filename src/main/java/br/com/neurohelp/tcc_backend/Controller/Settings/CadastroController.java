package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.DTO.CadastroProfissionalDTO;
import br.com.neurohelp.tcc_backend.DTO.CadastroResponsavelDTO;
import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.responsavelRepository;
import br.com.neurohelp.tcc_backend.Repository.AdminRepository;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import br.com.neurohelp.tcc_backend.Service.ConfirmacaoEmailService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import br.com.neurohelp.tcc_backend.Service.CpfValidator;
import br.com.neurohelp.tcc_backend.Service.CrmService;
import br.com.neurohelp.tcc_backend.Service.RecuperacaoLimites;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/cadastro")
public class CadastroController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final profissionalRepository profissionalRepository;
    private final responsavelRepository responsavelRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminRepository admins;
    private final ConfirmacaoEmailService confirmacao;
    private final CrmService crm;
    private final RecuperacaoLimites limites;

    public CadastroController(
            profissionalRepository profissionalRepository,
            responsavelRepository responsavelRepository,
            PasswordEncoder passwordEncoder, AdminRepository admins, ConfirmacaoEmailService confirmacao, CrmService crm, RecuperacaoLimites limites) {
        this.crm = crm;
        this.limites = limites;
        this.admins = admins;
        this.confirmacao = confirmacao;

        this.profissionalRepository = profissionalRepository;
        this.responsavelRepository = responsavelRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    @PostMapping("/profissional")
    public ResponseEntity<?> cadastrarProfissional(@Valid @RequestBody CadastroProfissionalDTO dto, HttpServletRequest request) {
        if (dto.cpf() != null && !dto.cpf().isBlank() && !CpfValidator.valido(dto.cpf())) {
            return ResponseEntity.badRequest().body("CPF inválido. Confira o número informado.");
        }
        if (dto.senha().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.badRequest().body("A senha deve ocupar no máximo 72 bytes.");
        }
        String email = normalizarEmail(dto.email());
        if (!emailValido(email)) {
            return ResponseEntity.badRequest().body("Dados de cadastro inválidos.");
        }
        if (emailEmUso(email)) {
            return ResponseEntity.status(409).body("Não foi possível concluir o cadastro com os dados informados.");
        }

        if (dto.numRegistro() != null && dto.numRegistro().stripLeading().toUpperCase(Locale.ROOT).startsWith("CRM"))
            limites.verificar("crm:" + request.getRemoteAddr(), 20);
        crm.validarRegistroSeCrm(dto.numRegistro(), dto.nome());
        confirmacao.consumir(email, dto.codigoEmail());
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

    @Transactional(noRollbackFor = ResponseStatusException.class)
    @PostMapping("/responsavel")
    public ResponseEntity<?> cadastrarResponsavel(@Valid @RequestBody CadastroResponsavelDTO dto) {
        if (dto.cpf() != null && !dto.cpf().isBlank() && !CpfValidator.valido(dto.cpf())) {
            return ResponseEntity.badRequest().body("CPF inválido. Confira o número informado.");
        }
        if (dto.senha().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.badRequest().body("A senha deve ocupar no máximo 72 bytes.");
        }
        String email = normalizarEmail(dto.email());
        if (!emailValido(email)) {
            return ResponseEntity.badRequest().body("Dados de cadastro inválidos.");
        }
        if (emailEmUso(email)) {
            return ResponseEntity.status(409).body("Não foi possível concluir o cadastro com os dados informados.");
        }

        confirmacao.consumir(email, dto.codigoEmail());
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
        return admins.findByEmailIgnoreCase(email).isPresent()
                || profissionalRepository.findByEmailIgnoreCase(email).isPresent()
                || responsavelRepository.findByEmailIgnoreCase(email).isPresent();
    }

    private boolean emailValido(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
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
