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
public class UserProf implements UsuarioAutenticavel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nome;

    private String bio;

    @Column(unique = true,  nullable = false)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(unique = true,nullable = false)
    private String cpf;

    private String telefone;

    private String estado;

    private String numRegistro;

    @Override
    public String getEmail() {
        return email;
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
