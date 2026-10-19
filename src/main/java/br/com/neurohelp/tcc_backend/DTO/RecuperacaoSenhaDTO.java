package br.com.neurohelp.tcc_backend.DTO;

import jakarta.validation.constraints.*;

public final class RecuperacaoSenhaDTO {
    private RecuperacaoSenhaDTO() {}
    public record Solicitar(@NotBlank @Email @Size(max = 254) String email) {}
    public record Redefinir(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "[0-9]{8}") String codigo,
            @NotBlank @Size(max = 72)
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{8,}$") String novaSenha) {}
}
