package br.com.neurohelp.tcc_backend.Entity.User;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    // Imagem compacta persistida; acessível apenas pela rota autenticada de foto.
    @JsonIgnore
    @Column(columnDefinition = "TEXT")
    private String fotoPerfil;

    private String bio;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String senha;

    // DEFAULT permite atualizar tabelas existentes sem invalidar contas antigas.
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long versaoCredenciais;

    @Column(unique = true, nullable = false)
    private String cpf;

    private String telefone;

    private String estado;

    private String cidade;

    private String cep;
    private String bairro;
    private String logradouro;
    private String numero;

    private String formacao;

    private String numRegistro;

    // Escrita exclusiva do fluxo autenticado de conferência; nunca aceitar estes campos no cadastro.
    @JsonIgnore @Column(length = 24)
    private String crmStatus;
    @JsonIgnore
    private String crmNomeConferido;
    @JsonIgnore
    private String crmRegistroConferido;
    @JsonIgnore
    private java.time.Instant crmSolicitadoEm;
    @JsonIgnore
    private java.time.Instant crmConferidoEm;
    @JsonIgnore
    private Long crmRevisorId;
    @JsonIgnore @Column(nullable = false, columnDefinition = "bigint default 0")
    private long crmRevisao;

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
