package br.com.neurohelp.tcc_backend.Controller.Perfil;

import br.com.neurohelp.tcc_backend.Entity.User.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conta/privacidade")
public class PrivacidadeController {
    public record Dados(String nome, String email, String cpf, String telefone, String estado,
                        String cidade, String cep, String bairro, String logradouro, String numero,
                        String formacao, String numRegistro) {}

    @GetMapping
    public ResponseEntity<Dados> consultar(@AuthenticationPrincipal UsuarioAutenticavel usuario) {
        Dados dados;
        if (usuario instanceof UserProf p) {
            dados = new Dados(p.getNome(), p.getEmail(), p.getCpf(), p.getTelefone(), p.getEstado(),
                    p.getCidade(), p.getCep(), p.getBairro(), p.getLogradouro(), p.getNumero(), p.getFormacao(), p.getNumRegistro());
        } else if (usuario instanceof UserResp r) {
            dados = new Dados(r.getNome(), r.getEmail(), r.getCpf(), r.getTelefone(), r.getEstado(),
                    r.getCidade(), r.getCep(), r.getBairro(), r.getLogradouro(), r.getNumero(), null, null);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(dados);
    }
}
