package br.com.neurohelp.tcc_backend.Entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Entity
public class UserResp implements UsuarioAutenticavel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nome;

    @Column(unique = true,nullable = false)
    private String email;

    private String telefone;

    private String estado;

    @Column(unique = true,nullable = false)
    private String cpf;

    @Column(nullable = false)
    private String senha;

    @Override
    public String getEmail() {
        return email    ;
    }

    @Override
    public String getSenha() {
        return senha;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
}

