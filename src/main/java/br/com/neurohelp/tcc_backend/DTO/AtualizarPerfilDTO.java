package br.com.neurohelp.tcc_backend.DTO;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizarPerfilDTO(
        @Size(max = 120) String nome,
        @Size(max = 2000) String bio,
        @Pattern(
                regexp = "^\\s*$|^(?=(?:[^0-9]*[0-9]){10,11}[^0-9]*$)[0-9()\\-\\s]+$",
                message = "Telefone deve possuir 10 ou 11 dígitos."
        )
        String telefone,
        @Size(max = 80) String estado,
        @Size(max = 120) String cidade,
        @Size(max = 160) String formacao,
        @Size(max = 80) String numRegistro
) {
}
