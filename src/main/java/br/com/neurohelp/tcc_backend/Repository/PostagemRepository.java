package br.com.neurohelp.tcc_backend.Repository;

import br.com.neurohelp.tcc_backend.Entity.Postagem.Postagem;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostagemRepository extends JpaRepository<Postagem, Long> {
    @Override
    @NonNull
    Optional<Postagem> findById(@NonNull Long aLong);

}
