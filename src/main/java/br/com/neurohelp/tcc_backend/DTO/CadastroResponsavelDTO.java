package br.com.neurohelp.tcc_backend.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroResponsavelDTO(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 254) String email,
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{8,}$",
                message = "A senha deve ter ao menos 8 caracteres, com maiúscula, minúscula, número e caractere especial."
        )
        String senha,
        @Pattern(
                regexp = "^\\s*$|^(?=(?:[^0-9]*[0-9]){11}[^0-9]*$)[0-9.\\-\\s]+$",
                message = "CPF deve possuir exatamente 11 dígitos."
        )
        String cpf,
        @Pattern(
                regexp = "^\\s*$|^(?=(?:[^0-9]*[0-9]){10,11}[^0-9]*$)[0-9()\\-\\s]+$",
                message = "Telefone deve possuir 10 ou 11 dígitos."
        )
        String telefone,
        @Size(max = 80) String estado,
        @Size(max = 120) String cidade,
        @Pattern(regexp = "^$|[0-9]{8}", message = "CEP deve possuir 8 dígitos.") String cep,
        @Size(max = 120) String bairro,
        @Size(max = 160) String logradouro,
        @Size(max = 20) String numero,
        @Size(max = 8) String codigoEmail
) {
}
