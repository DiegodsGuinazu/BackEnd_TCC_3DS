package br.com.neurohelp.tcc_backend.Service;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.responsavelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private responsavelRepository responsavelRepository;

    @Autowired
    private profissionalRepository profissionalRepository;

    public Object buscarPorEmail(String email) {

        Optional<UserResp> resp = responsavelRepository.findByEmail(email);
        if (resp.isPresent()) {
            return resp.get();
        }

        Optional<UserProf> prof = profissionalRepository.findByEmail(email);
        return prof.orElse(null);

    }
}
