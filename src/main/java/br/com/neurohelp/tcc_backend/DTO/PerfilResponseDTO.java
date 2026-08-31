package br.com.neurohelp.tcc_backend.DTO;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;

public record PerfilResponseDTO(
        String id,
        String nome,
        String email,
        String bio,
        String cpf,
        String telefone,
        String estado,
        String numRegistro
) {
    public PerfilResponseDTO(UserProf userProf){
        this(
                String.valueOf(userProf.getId()),
                userProf.getNome(),
                userProf.getEmail(),
                userProf.getBio(),
                userProf.getCpf(),
                userProf.getEstado(),
                userProf.getTelefone(),
                userProf.getNumRegistro()


        );
    }
    @GetMapping
    public ResponseEntity<PerfilResponseDTO> buscarPerfil(@AuthenticationPrincipal UserProf usuarioLogado) {
        // 'usuarioLogado' já vem preenchido diretamente pelo filtro!
        return ResponseEntity.ok(new PerfilResponseDTO(usuarioLogado));
    }
}
