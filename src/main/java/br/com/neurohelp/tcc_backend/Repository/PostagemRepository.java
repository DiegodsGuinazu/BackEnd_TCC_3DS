package br.com.neurohelp.tcc_backend.Repository;

import br.com.neurohelp.tcc_backend.Entity.Postagem.Postagem;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PostagemRepository extends JpaRepository<Postagem, Long> {
    @EntityGraph(attributePaths = "categorias")
    List<Postagem> findByPublicadoAprendizagemTrueOrderByDataCriacaoDescIdDesc();

    @EntityGraph(attributePaths = "categorias")
    Optional<Postagem> findByIdAndPublicadoAprendizagemTrue(Long id);
    @Override
    @NonNull
    Optional<Postagem> findById(@NonNull Long aLong);

}
