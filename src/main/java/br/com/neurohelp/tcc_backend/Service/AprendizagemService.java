package br.com.neurohelp.tcc_backend.Service;

import br.com.neurohelp.tcc_backend.DTO.AprendizagemResponseDTO;
import br.com.neurohelp.tcc_backend.DTO.AprendizagemResumoResponseDTO;
import br.com.neurohelp.tcc_backend.Entity.Postagem.Categoria;
import br.com.neurohelp.tcc_backend.Entity.Postagem.Postagem;
import br.com.neurohelp.tcc_backend.Repository.PostagemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class AprendizagemService {
    private final PostagemRepository repository;

    public AprendizagemService(PostagemRepository repository) {
        this.repository = repository;
    }

    public List<AprendizagemResumoResponseDTO> listar() {
        return repository.findByPublicadoAprendizagemTrueOrderByDataCriacaoDescIdDesc().stream()
                .map(postagem -> new AprendizagemResumoResponseDTO(postagem.getId(), postagem.getTitulo(),
                        postagem.getDataCriacao(), postagem.getDataAtualizacao(), categorias(postagem)))
                .toList();
    }

    public AprendizagemResponseDTO buscar(Long id) {
        Postagem postagem = repository.findByIdAndPublicadoAprendizagemTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artigo não encontrado."));
        return new AprendizagemResponseDTO(postagem.getId(), postagem.getTitulo(), postagem.getConteudoHtml(),
                postagem.getDataCriacao(), postagem.getDataAtualizacao(), categorias(postagem));
    }

    private List<String> categorias(Postagem postagem) {
        if (postagem.getCategorias() == null) return List.of();
        return postagem.getCategorias().stream().map(Categoria::getNome)
                .filter(Objects::nonNull).sorted().toList();
    }
}
