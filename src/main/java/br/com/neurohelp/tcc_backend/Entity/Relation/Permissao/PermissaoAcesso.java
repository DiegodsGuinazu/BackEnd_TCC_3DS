package br.com.neurohelp.tcc_backend.Entity.Relation.Permissao;

import br.com.neurohelp.tcc_backend.Entity.Postagem.Postagem;
import jakarta.persistence.*;

@Entity
@Table(name ="permissao_acesso")
public class PermissaoAcesso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "postagem_id")
        private Postagem postagem;
}
