package br.com.neurohelp.tcc_backend.Entity.User;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Getter
@Setter
public class ConviteAdmin {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(nullable = false)
    private Instant expiraEm;
    private Instant utilizadoEm;
    private Instant revogadoEm;
}
