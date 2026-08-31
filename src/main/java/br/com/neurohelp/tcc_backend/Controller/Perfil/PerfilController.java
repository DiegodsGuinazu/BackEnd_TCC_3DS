package br.com.neurohelp.tcc_backend.Controller.Perfil;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.DTO.AtualizarPerfilDTO;
import br.com.neurohelp.tcc_backend.DTO.PerfilResponseDTO;
import br.com.neurohelp.tcc_backend.Repository.ProfissionalRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final ProfissionalRepository profissionalRepository;

    public PerfilController(ProfissionalRepository profissionalRepository) {
        this.profissionalRepository = profissionalRepository;
    }

    // Retorna os dados do perfil logado
    @GetMapping
    public ResponseEntity<PerfilResponseDTO> buscarPerfil(@AuthenticationPrincipal UserProf usuarioLogado) {
        return ResponseEntity.ok(new PerfilResponseDTO(usuarioLogado));
    }

    // Atualiza os dados do perfil logado
    @PutMapping
    public ResponseEntity<PerfilResponseDTO> atualizarPerfil(
            @AuthenticationPrincipal UserProf usuarioLogado,
            @RequestBody AtualizarPerfilDTO dto) {

        // Atualiza apenas os campos enviados
        if (dto.nome() != null) usuarioLogado.setNome(dto.nome());
        if (dto.bio() != null) usuarioLogado.setBio(dto.bio());
        if (dto.telefone() != null) usuarioLogado.setTelefone(dto.telefone());
        if (dto.estado() != null) usuarioLogado.setEstado(dto.estado());
        if (dto.numRegistro() != null) usuarioLogado.setNumRegistro(dto.numRegistro());

        // Salva as alterações no banco de dados
        UserProf usuarioAtualizado = profissionalRepository.save(usuarioLogado);

        return ResponseEntity.ok(new PerfilResponseDTO(usuarioAtualizado));
    }
}