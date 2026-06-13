package br.com.neurohelp.tcc_backend.Service;

import br.com.neurohelp.tcc_backend.Entity.Postagem.Postagem;
import br.com.neurohelp.tcc_backend.Repository.PostagemRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PostagemService {
    private static PostagemRepository postagemRepository;

    public static void setPublicacaoRepository(PostagemRepository postagemRepository) {
        PostagemService.postagemRepository = postagemRepository;
    }

    //procurar por ID
    public Optional<Postagem> findByID(Long id) { return postagemRepository.findById(id); }
    public static Postagem salvar(Postagem postagem) { return postagemRepository.save(postagem); }
}
