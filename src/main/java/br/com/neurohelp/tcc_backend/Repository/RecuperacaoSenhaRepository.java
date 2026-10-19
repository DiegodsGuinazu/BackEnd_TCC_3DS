package br.com.neurohelp.tcc_backend.Repository;

import br.com.neurohelp.tcc_backend.Entity.User.RecuperacaoSenha;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecuperacaoSenha r where r.email = :email")
    Optional<RecuperacaoSenha> buscarComBloqueio(@org.springframework.data.repository.query.Param("email") String email);
}
