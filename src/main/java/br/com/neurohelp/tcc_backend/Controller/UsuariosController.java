package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.DTO.Login;
import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Entity.User.UsuarioAutenticavel;
import br.com.neurohelp.tcc_backend.Repository.ProfissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.ResponsavelRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;


@RestController
@RequestMapping("/usuarios")
public class UsuariosController {

    @Autowired
    private ResponsavelRepository respRepository;

    @Autowired
    private ProfissionalRepository profRepository;

    @PostMapping("/autenticar")
    public ResponseEntity<Boolean> autentica(@RequestBody Login login) {

        UserResp resp = (respRepository.findByEmail(login.getEmail()).orElse(null));

        if (autenticarUsuario(resp, login)) {
            return ResponseEntity.ok(true);
        }

        UserProf prof = profRepository.findByEmail(login.getEmail()).orElse(null);

        if (autenticarUsuario(prof, login)) {
            return ResponseEntity.ok(true);
        }

        return ResponseEntity.ok(false);
    }

    private boolean autenticarUsuario(
            UsuarioAutenticavel usuario,
            Login login) {

        return usuario != null &&
                usuario.getSenha().equals(login.getSenha());
    }
}
