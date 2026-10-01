package br.com.neurohelp.tcc_backend.DTO;

import br.com.neurohelp.tcc_backend.Entity.User.UserResp;

public record PerfilResponseRespDTO(
        String id,
        String nome,
        String email,
        String cpf,
        String telefone,
        String estado,
        String cidade
) {
    public PerfilResponseRespDTO(UserResp userResp) {
        this(
                String.valueOf(userResp.getId()),
                userResp.getNome(),
                userResp.getEmail(),
                userResp.getCpf(),
                userResp.getTelefone(),
                userResp.getEstado(),
                userResp.getCidade()
        );
    }
}
