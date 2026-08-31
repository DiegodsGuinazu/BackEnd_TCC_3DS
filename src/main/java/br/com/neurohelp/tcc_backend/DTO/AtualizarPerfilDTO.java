package br.com.neurohelp.tcc_backend.DTO;

public record AtualizarPerfilDTO(
        String nome,
        String bio,
        String telefone,
        String estado,
        String numRegistro
) {

}
