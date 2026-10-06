package br.com.neurohelp.tcc_backend.DTO;

import jakarta.validation.constraints.*;

public record ConviteEmailDTO(
        @NotBlank @Size(max = 254) @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") String email) {
}
