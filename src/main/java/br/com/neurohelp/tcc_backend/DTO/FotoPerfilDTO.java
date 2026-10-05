package br.com.neurohelp.tcc_backend.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FotoPerfilDTO(
        @NotBlank @Size(max = 180000) String fotoPerfil
) {}
