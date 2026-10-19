package br.com.neurohelp.tcc_backend.Entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** Nenhum código de recuperação é armazenado em texto puro. */
@Entity @Getter @Setter
public class RecuperacaoSenha {
    @Id @Column(length = 64) private String emailHash;
    @Column(nullable = false, length = 64) private String codigoHash;
    @Column(nullable = false) private Instant expiraEm;
    @Column(nullable = false) private Instant enviadoEm;
    @Column(nullable = false) private int tentativas;
    @Column(nullable = false) private long versaoCredenciais;
    @Column(nullable = false) private boolean utilizado;
}
