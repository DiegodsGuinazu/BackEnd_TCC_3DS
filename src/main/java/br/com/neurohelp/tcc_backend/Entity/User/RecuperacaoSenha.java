package br.com.neurohelp.tcc_backend.Entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Getter @Setter
public class RecuperacaoSenha {
    @Id @Column(length = 254) private String email;
    @Column(nullable = false, length = 64) private String codigoHash;
    @Column(nullable = false) private Instant expiraEm;
    @Column(nullable = false) private Instant solicitadoEm;
    @Column(nullable = false) private int tentativas;
    @Column(nullable = false) private int tokenVersion;
    @Column(nullable = false) private String tipoPerfil;
    @Column(nullable = false) private int usuarioId;
    @Column(nullable = false) private boolean usado;
}
