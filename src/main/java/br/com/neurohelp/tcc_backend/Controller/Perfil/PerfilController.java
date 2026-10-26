package br.com.neurohelp.tcc_backend.Controller.Perfil;

import br.com.neurohelp.tcc_backend.DTO.AtualizarPerfilDTO;
import br.com.neurohelp.tcc_backend.DTO.PerfilResponseDTO;
import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import jakarta.validation.Valid;
import br.com.neurohelp.tcc_backend.Service.CrmService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final profissionalRepository profissionalRepository;

    private final CrmService crm;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public PerfilController(profissionalRepository profissionalRepository, CrmService crm) {
        this.crm = crm;
        this.profissionalRepository = profissionalRepository;
    }

    @GetMapping
    public ResponseEntity<PerfilResponseDTO> buscarPerfil(@AuthenticationPrincipal UserProf usuarioLogado) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(new PerfilResponseDTO(usuarioLogado));
    }

    @PutMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<PerfilResponseDTO> atualizarPerfil(
            @AuthenticationPrincipal UserProf usuarioLogado,
            @Valid @RequestBody AtualizarPerfilDTO dto) {

        usuarioLogado = profissionalRepository.buscarParaConferencia(usuarioLogado.getId()).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Perfil não encontrado."));
        entityManager.refresh(usuarioLogado, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        String registro = dto.numRegistro() == null ? usuarioLogado.getNumRegistro() : normalizarTextoOpcional(dto.numRegistro());
        String nome = dto.nome() == null ? usuarioLogado.getNome() : dto.nome().trim();
        // Apenas formato local; cadastro/edição não dependem de consulta externa.
        if (!Objects.equals(registro, usuarioLogado.getNumRegistro()) || !Objects.equals(nome, usuarioLogado.getNome())) {
            crm.validarRegistroSeCrm(registro, nome);
            usuarioLogado.setCrmStatus(null);
            usuarioLogado.setCrmNomeConferido(null);
            usuarioLogado.setCrmRegistroConferido(null);
            usuarioLogado.setCrmConferidoEm(null);
            usuarioLogado.setCrmRevisorId(null);
            usuarioLogado.setCrmRevisao(usuarioLogado.getCrmRevisao() + 1);
        }

        if (dto.nome() != null) usuarioLogado.setNome(dto.nome().trim());
        if (dto.bio() != null) usuarioLogado.setBio(normalizarTextoOpcional(dto.bio()));
        if (dto.telefone() != null) usuarioLogado.setTelefone(normalizarTelefone(dto.telefone()));
        if (dto.estado() != null) usuarioLogado.setEstado(normalizarTextoOpcional(dto.estado()));
        if (dto.cidade() != null) usuarioLogado.setCidade(normalizarTextoOpcional(dto.cidade()));
        if (dto.formacao() != null) usuarioLogado.setFormacao(normalizarTextoOpcional(dto.formacao()));
        if (dto.numRegistro() != null) usuarioLogado.setNumRegistro(normalizarTextoOpcional(dto.numRegistro()));

        UserProf usuarioAtualizado = profissionalRepository.save(usuarioLogado);
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(new PerfilResponseDTO(usuarioAtualizado));
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }

    private String normalizarTelefone(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.replaceAll("\\D", "");
    }
}
