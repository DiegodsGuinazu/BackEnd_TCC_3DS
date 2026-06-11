package br.com.neurohelp.tcc_backend.Entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
}

