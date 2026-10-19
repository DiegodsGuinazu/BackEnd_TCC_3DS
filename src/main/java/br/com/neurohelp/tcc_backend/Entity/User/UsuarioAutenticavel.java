package br.com.neurohelp.tcc_backend.Entity.User;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public interface UsuarioAutenticavel {
    String getEmail();
    String getSenha();
    default int getTokenVersion() { return 0; }

    Collection<? extends GrantedAuthority> getAuthorities();
}
