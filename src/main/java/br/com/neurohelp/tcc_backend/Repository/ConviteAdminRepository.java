package br.com.neurohelp.tcc_backend.Repository;

import br.com.neurohelp.tcc_backend.Entity.User.ConviteAdmin;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ConviteAdminRepository extends JpaRepository<ConviteAdmin, Long> {
    Optional<ConviteAdmin> findByTokenHash(String hash);
    List<ConviteAdmin> findAllByOrderByIdDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConviteAdmin c where c.tokenHash = :hash")
    Optional<ConviteAdmin> bloquearPorHash(@Param("hash") String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConviteAdmin c where c.id = :id")
    Optional<ConviteAdmin> bloquearPorId(@Param("id") Long id);
}
