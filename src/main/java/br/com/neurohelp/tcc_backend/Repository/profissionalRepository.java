package br.com.neurohelp.tcc_backend.Repository;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface profissionalRepository extends JpaRepository<UserProf, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from UserProf p where p.id = :id")
    Optional<UserProf> buscarParaConferencia(@org.springframework.data.repository.query.Param("id") int id);
    org.springframework.data.domain.Page<UserProf> findByCrmStatusOrderByCrmSolicitadoEmAsc(String status, org.springframework.data.domain.Pageable pagina);
    Optional<UserProf> findByEmail(String email);
    Optional<UserProf> findByEmailIgnoreCase(String email);
}
