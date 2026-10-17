package br.com.neurohelp.tcc_backend.DTO;

import jakarta.validation.constraints.*;

// Somente campos editáveis; IDs, publicação, categorias e anexos não vêm da entidade do cliente.
public record PostagemEdicaoDTO(@NotBlank @Size(max = 255) String titulo,
                               @NotBlank @Size(max = 100000) String conteudoHtml,
                               @Size(max = 2000) String resumo,
                               @Size(max = 20000) String referencias,
                               @Size(max = 2800000) String imagemData) {}
