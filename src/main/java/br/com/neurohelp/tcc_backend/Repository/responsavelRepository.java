package br.com.neurohelp.tcc_backend.Repository;

import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface responsavelRepository extends JpaRepository<UserResp, Long> {
    Optional<UserResp> findByEmail(String email);
    Optional<UserResp> findByEmailIgnoreCase(String email);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from UserResp u where lower(u.email) = lower(:email)")
    Optional<UserResp> buscarParaRecuperacao(@org.springframework.data.repository.query.Param("email") String email);
}

