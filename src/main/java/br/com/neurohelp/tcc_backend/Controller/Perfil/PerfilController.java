package br.com.neurohelp.tcc_backend.Controller.Perfil;

import br.com.neurohelp.tcc_backend.DTO.AtualizarPerfilDTO;
import br.com.neurohelp.tcc_backend.DTO.PerfilResponseDTO;
import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final profissionalRepository profissionalRepository;

    public PerfilController(profissionalRepository profissionalRepository) {
        this.profissionalRepository = profissionalRepository;
    }

    @GetMapping
    public ResponseEntity<PerfilResponseDTO> buscarPerfil(@AuthenticationPrincipal UserProf usuarioLogado) {
        return ResponseEntity.ok(new PerfilResponseDTO(usuarioLogado));
    }

    @PutMapping
    public ResponseEntity<PerfilResponseDTO> atualizarPerfil(
            @AuthenticationPrincipal UserProf usuarioLogado,
            @Valid @RequestBody AtualizarPerfilDTO dto) {

        if (dto.nome() != null) usuarioLogado.setNome(dto.nome().trim());
        if (dto.bio() != null) usuarioLogado.setBio(normalizarTextoOpcional(dto.bio()));
        if (dto.telefone() != null) usuarioLogado.setTelefone(normalizarTelefone(dto.telefone()));
        if (dto.estado() != null) usuarioLogado.setEstado(normalizarTextoOpcional(dto.estado()));
        if (dto.cidade() != null) usuarioLogado.setCidade(normalizarTextoOpcional(dto.cidade()));
        if (dto.formacao() != null) usuarioLogado.setFormacao(normalizarTextoOpcional(dto.formacao()));
        if (dto.numRegistro() != null) usuarioLogado.setNumRegistro(normalizarTextoOpcional(dto.numRegistro()));

        UserProf usuarioAtualizado = profissionalRepository.save(usuarioLogado);
        return ResponseEntity.ok(new PerfilResponseDTO(usuarioAtualizado));
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
