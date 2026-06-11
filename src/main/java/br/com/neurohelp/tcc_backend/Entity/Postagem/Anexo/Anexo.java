package br.com.neurohelp.tcc_backend.Entity.Postagem.Anexo;

import br.com.neurohelp.tcc_backend.Entity.Postagem.Postagem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class Anexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "Postagem_id")
    private Postagem postagem;

    private String nomeOriginal;

    private String tipoArquivo;

    private String urlArquivo;

    private Long TamanhoArquivo;

    private LocalDateTime dataUpload;
}
