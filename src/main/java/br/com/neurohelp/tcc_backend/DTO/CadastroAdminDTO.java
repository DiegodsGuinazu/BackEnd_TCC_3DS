package br.com.neurohelp.tcc_backend.DTO;

import jakarta.validation.constraints.*;

public record CadastroAdminDTO(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 254) @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") String email,
        @NotBlank @Size(max = 72)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{8,}$",
                message = "Use ao menos 8 caracteres, maiúscula, minúscula, número e símbolo.") String senha,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token) {
}
