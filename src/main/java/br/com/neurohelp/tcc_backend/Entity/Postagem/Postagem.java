package br.com.neurohelp.tcc_backend.Entity.Postagem;

import br.com.neurohelp.tcc_backend.Entity.Postagem.Anexo.Anexo;
import br.com.neurohelp.tcc_backend.Entity.Relation.Permissao.PermissaoAcesso;
import br.com.neurohelp.tcc_backend.res.tipoPostagem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Postagem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private LocalDateTime dataCriacao;

    private LocalDateTime dataAtualizacao;

    @Column(columnDefinition = "TEXT")
    private String conteudoHtml;

    private String titulo;

    @OneToMany(mappedBy = "postagem", cascade = CascadeType.ALL,  orphanRemoval = true)
    private List<PermissaoAcesso> permissoes = new ArrayList<>();

    @OneToMany(mappedBy = "postagem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Anexo> anexos = new ArrayList<>();

    @ElementCollection(targetClass = tipoPostagem.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "postagem_tipo",
            joinColumns = @JoinColumn(name = "postagem_id")
    )
    @Column(name = "tipo")
    private List<tipoPostagem> tipos;

    @ElementCollection(targetClass = Categoria.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "postagem_categoria",
            joinColumns = @JoinColumn(name = "postagem_id")
    )
    @Column(name = "categoria")
    private Set<Categoria> categorias;

}
