package br.com.neurohelp.tcc_backend.DTO;

import java.time.LocalDateTime;
import java.util.List;

public record AprendizagemResponseDTO(
        long id, String titulo, String conteudoHtml, LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao, List<String> categorias) {
}
