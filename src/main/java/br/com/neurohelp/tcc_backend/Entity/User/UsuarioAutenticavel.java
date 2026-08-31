package br.com.neurohelp.tcc_backend.Entity.User;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public interface UsuarioAutenticavel {
    String getEmail();
    String getSenha();

    Collection<? extends GrantedAuthority> getAuthorities();
}
