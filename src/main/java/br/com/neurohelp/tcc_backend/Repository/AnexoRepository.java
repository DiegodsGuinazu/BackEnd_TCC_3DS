package br.com.neurohelp.tcc_backend.Repository;

import br.com.neurohelp.tcc_backend.Entity.Postagem.Anexo.Anexo;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnexoRepository extends JpaRepository<Anexo, Long> {
    @Override
    @Nonnull
    Optional<Anexo> findById(@Nonnull Long aLong);
}
