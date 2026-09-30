package br.com.neurohelp.tcc_backend.Controller.Settings;

import br.com.neurohelp.tcc_backend.DTO.UsuarioLogadoResponseDTO;
import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Entity.User.UsuarioAutenticavel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class SessaoController {
    @GetMapping("/me")
    public UsuarioLogadoResponseDTO usuarioLogado(@AuthenticationPrincipal UsuarioAutenticavel usuario) {
        if (usuario instanceof UserProf profissional) {
            return new UsuarioLogadoResponseDTO(String.valueOf(profissional.getId()),
                    profissional.getNome(), profissional.getEmail(), "PROFISSIONAL");
        }
        if (usuario instanceof UserResp responsavel) {
            return new UsuarioLogadoResponseDTO(String.valueOf(responsavel.getId()),
                    responsavel.getNome(), responsavel.getEmail(), "RESPONSAVEL");
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
}
