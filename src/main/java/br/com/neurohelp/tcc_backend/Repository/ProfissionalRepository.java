package br.com.neurohelp.tcc_backend.Repository;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository

public interface ProfissionalRepository extends JpaRepository<UserProf, Long> {

    Optional<UserProf> findByEmail(String email);

}
