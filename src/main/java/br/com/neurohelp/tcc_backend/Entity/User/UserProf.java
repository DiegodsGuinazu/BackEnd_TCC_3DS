package br.com.neurohelp.tcc_backend.Entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class UserProf implements UsuarioAutenticavel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nome;

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
}
