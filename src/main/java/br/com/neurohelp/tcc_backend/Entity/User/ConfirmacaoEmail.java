package br.com.neurohelp.tcc_backend.Entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "confirmacao_email")
@Getter @Setter
public class ConfirmacaoEmail {
    @Id @Column(length = 64) private String emailHash;
    @Column(nullable = false, length = 64) private String codigoHash;
    @Column(nullable = false) private Instant enviadoEm;
    @Column(nullable = false) private Instant expiraEm;
    @Column(nullable = false) private int tentativas;
    @Column(nullable = false) private boolean utilizado;
}
