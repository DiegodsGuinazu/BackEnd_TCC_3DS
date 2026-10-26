package br.com.neurohelp.tcc_backend.DTO;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;

public record PerfilResponseDTO(
        String id,
        String nome,
        String email,
        String bio,
        String cpf,
        String telefone,
        String estado,
        String cidade,
        String formacao,
        String numRegistro,
        br.com.neurohelp.tcc_backend.Service.CrmService.Verificacao crmVerificacao
) {
    public PerfilResponseDTO(UserProf userProf) {
        this(
                String.valueOf(userProf.getId()),
                userProf.getNome(),
                userProf.getEmail(),
                userProf.getBio(),
                userProf.getCpf(),
                userProf.getTelefone(),
                userProf.getEstado(),
                userProf.getCidade(),
                userProf.getFormacao(),
                userProf.getNumRegistro(),
                br.com.neurohelp.tcc_backend.Service.CrmService.verificacao(userProf)
        );
    }
}
