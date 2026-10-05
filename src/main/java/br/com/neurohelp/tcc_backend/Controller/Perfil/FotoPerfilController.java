package br.com.neurohelp.tcc_backend.Controller.Perfil;

import br.com.neurohelp.tcc_backend.DTO.FotoPerfilDTO;
import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Entity.User.UsuarioAutenticavel;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.responsavelRepository;
import br.com.neurohelp.tcc_backend.Service.FotoPerfilService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/conta/foto")
public class FotoPerfilController {
    private final profissionalRepository profissionais;
    private final responsavelRepository responsaveis;
    private final FotoPerfilService service;

    public FotoPerfilController(profissionalRepository profissionais, responsavelRepository responsaveis,
                                FotoPerfilService service) {
        this.profissionais = profissionais;
        this.responsaveis = responsaveis;
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<FotoPerfilDTO> buscar(@AuthenticationPrincipal UsuarioAutenticavel usuario) {
        String foto;
        if (usuario instanceof UserProf profissional) foto = profissional.getFotoPerfil();
        else if (usuario instanceof UserResp responsavel) foto = responsavel.getFotoPerfil();
        else throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return resposta(foto);
    }

    @PutMapping
    public ResponseEntity<FotoPerfilDTO> atualizar(@AuthenticationPrincipal UsuarioAutenticavel usuario,
                                                 @Valid @RequestBody FotoPerfilDTO dto) {
        String foto = service.normalizar(dto.fotoPerfil());
        // Não recebe ID/email: o destino é sempre a identidade validada pelo JWT.
        if (usuario instanceof UserProf profissional) {
            profissional.setFotoPerfil(foto);
            profissionais.save(profissional);
        } else if (usuario instanceof UserResp responsavel) {
            responsavel.setFotoPerfil(foto);
            responsaveis.save(responsavel);
        } else throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return resposta(foto);
    }

    private ResponseEntity<FotoPerfilDTO> resposta(String foto) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new FotoPerfilDTO(foto));
    }
}
