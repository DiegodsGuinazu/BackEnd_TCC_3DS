package br.com.neurohelp.tcc_backend.Repository;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface profissionalRepository extends JpaRepository<UserProf, Long> {
    Optional<UserProf> findByEmail(String email);
    Optional<UserProf> findByEmailIgnoreCase(String email);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from UserProf u where lower(u.email) = lower(:email)")
    Optional<UserProf> buscarParaRecuperacao(@org.springframework.data.repository.query.Param("email") String email);
}

