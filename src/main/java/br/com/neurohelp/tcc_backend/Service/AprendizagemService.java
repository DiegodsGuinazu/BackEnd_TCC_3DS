package br.com.neurohelp.tcc_backend.Service;

import br.com.neurohelp.tcc_backend.DTO.AprendizagemResponseDTO;
import br.com.neurohelp.tcc_backend.DTO.AprendizagemResumoResponseDTO;
import br.com.neurohelp.tcc_backend.DTO.PostagemEdicaoDTO;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDateTime;
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

    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public AprendizagemResponseDTO criar(PostagemEdicaoDTO dto) {
        Postagem postagem = new Postagem();
        aplicar(postagem, dto);
        postagem.setDataCriacao(LocalDateTime.now());
        postagem.setPublicadoAprendizagem(true);
        repository.saveAndFlush(postagem);
        return buscar(postagem.getId());
    }

    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public AprendizagemResponseDTO editar(Long id, PostagemEdicaoDTO dto) {
        Postagem postagem = publicada(id);
        aplicar(postagem, dto);
        // Mantém anexos e categorias existentes, sem recriar entidades ou aceitar autoria do cliente.
        repository.saveAndFlush(postagem);
        return buscar(postagem.getId());
    }

    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public void excluir(Long id) {
        Postagem postagem = publicada(id);
        postagem.getCategorias().clear();
        // cascade ALL/orphanRemoval remove anexos; categorias compartilhadas não são excluídas.
        repository.delete(postagem);
        repository.flush();
    }

    private Postagem publicada(Long id) {
        return repository.findByIdAndPublicadoAprendizagemTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artigo não encontrado."));
    }

    private void aplicar(Postagem postagem, PostagemEdicaoDTO dto) {
        String html = Jsoup.clean(dto.conteudoHtml(), new Safelist().addTags(
                "p", "br", "strong", "em", "b", "i", "ul", "ol", "li", "h2", "h3", "h4", "blockquote"));
        if (Jsoup.parse(html).text().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "O conteúdo deve conter texto.");
        postagem.setTitulo(dto.titulo().trim());
        postagem.setConteudoHtml(html);
        postagem.setDataAtualizacao(LocalDateTime.now());
    }

    private List<String> categorias(Postagem postagem) {
        if (postagem.getCategorias() == null) return List.of();
        return postagem.getCategorias().stream().map(Categoria::getNome)
                .filter(Objects::nonNull).sorted().toList();
    }
}
