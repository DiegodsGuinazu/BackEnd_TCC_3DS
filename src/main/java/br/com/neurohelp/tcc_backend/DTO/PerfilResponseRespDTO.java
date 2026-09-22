package br.com.neurohelp.tcc_backend.DTO;

import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;

public record PerfilResponseRespDTO(
        String id,
        String nome,
        String email,
        String cpf,
        String telefone,
        String estado
        ) {
    public PerfilResponseRespDTO(UserResp userResp){
        this(
                String.valueOf(userResp.getId()),
                userResp.getNome(),
                userResp.getEmail(),
                userResp.getCpf(),
                userResp.getTelefone(),
                userResp.getEstado()
        );
    }

    @GetMapping
    public ResponseEntity<PerfilResponseRespDTO> buscarPerfil(@AuthenticationPrincipal UserResp usuarioLogado) {
        return ResponseEntity.ok(new PerfilResponseRespDTO(usuarioLogado));
    }
}
