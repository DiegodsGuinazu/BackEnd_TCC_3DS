package br.com.neurohelp.tcc_backend.Repository;

import br.com.neurohelp.tcc_backend.Entity.User.ConfirmacaoEmail;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface ConfirmacaoEmailRepository extends JpaRepository<ConfirmacaoEmail, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConfirmacaoEmail c where c.emailHash = :hash")
    Optional<ConfirmacaoEmail> buscarComBloqueio(@Param("hash") String hash);

    @Modifying
    @Query("delete from ConfirmacaoEmail c where c.expiraEm < :limite")
    int removerExpirados(@Param("limite") Instant limite);
}
