package br.com.neurohelp.tcc_backend.Controller.Perfil;

import br.com.neurohelp.tcc_backend.DTO.AtualizarPerfilDTO;
import br.com.neurohelp.tcc_backend.DTO.PerfilResponseRespDTO;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Repository.responsavelRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil-responsavel") // Alterado para evitar conflito com /api/perfil
public class PerfilRespController {

    private final responsavelRepository responsavelRepository;

    public PerfilRespController(responsavelRepository responsavelRepository) {
        this.responsavelRepository = responsavelRepository;
    }

    @GetMapping
    public ResponseEntity<PerfilResponseRespDTO> buscarPerfil(@AuthenticationPrincipal UserResp usuarioLogado) {
        return ResponseEntity.ok(new PerfilResponseRespDTO(usuarioLogado));
    }

    @PutMapping
    public ResponseEntity<PerfilResponseRespDTO> atualizarPerfil(
            @AuthenticationPrincipal UserResp usuarioLogado,
            @RequestBody AtualizarPerfilDTO dto) {

        if (dto.nome() != null) usuarioLogado.setNome(dto.nome());
        if (dto.telefone() != null) usuarioLogado.setTelefone(dto.telefone());
        if (dto.estado() != null) usuarioLogado.setEstado(dto.estado());

        UserResp usuarioAtualizado = responsavelRepository.save(usuarioLogado);

        return ResponseEntity.ok(new PerfilResponseRespDTO(usuarioAtualizado));
    }
}