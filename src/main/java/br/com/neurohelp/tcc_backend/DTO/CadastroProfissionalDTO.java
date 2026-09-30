package br.com.neurohelp.tcc_backend.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CadastroProfissionalDTO(
        String nome, @NotBlank @Email String email, @NotBlank String senha,
        @NotBlank String cpf, String telefone, String estado, String bio, String numRegistro) {
}
