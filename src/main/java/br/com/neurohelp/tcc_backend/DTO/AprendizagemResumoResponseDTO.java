package br.com.neurohelp.tcc_backend.DTO;

import java.time.LocalDateTime;
import java.util.List;

public record AprendizagemResumoResponseDTO(
        long id, String titulo, LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao, List<String> categorias) {
}
