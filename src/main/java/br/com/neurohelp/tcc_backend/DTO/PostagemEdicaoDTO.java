package br.com.neurohelp.tcc_backend.DTO;

import jakarta.validation.constraints.*;

// Somente campos editáveis; IDs, publicação, categorias e anexos não vêm da entidade do cliente.
public record PostagemEdicaoDTO(@NotBlank @Size(max = 255) String titulo,
                               @NotBlank @Size(max = 100000) String conteudoHtml) {}
