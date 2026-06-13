package br.com.neurohelp.tcc_backend.Controller.Publicacao;


import br.com.neurohelp.tcc_backend.Entity.Postagem.Postagem;
import br.com.neurohelp.tcc_backend.Service.AnexoService;
import br.com.neurohelp.tcc_backend.Service.PostagemService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/publicacao")
public class PublicacaoController {

    @PostMapping("/salvar")
    public ResponseEntity<?> salvar(@RequestBody Postagem postagem){
        postagem.getAnexos().forEach(anexo -> anexo.setPostagem(postagem));
        postagem.getAnexos().forEach(AnexoService:: salvar);
        return ResponseEntity.ok((PostagemService.salvar(postagem)));
    }
}
