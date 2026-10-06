package br.com.neurohelp.tcc_backend.Repository;

import br.com.neurohelp.tcc_backend.Entity.User.UserAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<UserAdmin, Long> {
    Optional<UserAdmin> findByEmailIgnoreCase(String email);
}
