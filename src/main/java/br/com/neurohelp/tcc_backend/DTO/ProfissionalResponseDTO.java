package br.com.neurohelp.tcc_backend.DTO;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;

// Dados de apresentação; CPF, email e senha pertencem ao perfil privado.
public record ProfissionalResponseDTO(
        String id,
        String nome,
        String bio,
        String telefone,
        String estado,
        String cidade,
        String formacao,
        String numRegistro,
        String fotoPerfilUrl
) {
    public ProfissionalResponseDTO(UserProf usuario) {
        this(
                String.valueOf(usuario.getId()),
                usuario.getNome(),
                usuario.getBio(),
                usuario.getTelefone(),
                usuario.getEstado(),
                usuario.getCidade(),
                usuario.getFormacao(),
                usuario.getNumRegistro(),
                usuario.getFotoPerfil() == null ? null : "/api/profissionais/" + usuario.getId() + "/foto"
        );
    }
}
