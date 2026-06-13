package br.com.neurohelp.tcc_backend.Service;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Entity.User.UserResp;
import br.com.neurohelp.tcc_backend.Repository.ProfissionalRepository;
import br.com.neurohelp.tcc_backend.Repository.ResponsavelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private ResponsavelRepository responsavelRepository;

    @Autowired
    private ProfissionalRepository profissionalRepository;

    public Object buscarPorEmail(String email) {

        Optional<UserResp> resp = responsavelRepository.findByEmail(email);
        if (resp.isPresent()) {
            return resp.get();
        }

        Optional<UserProf> prof = profissionalRepository.findByEmail(email);
        if (prof.isPresent()) {
            return prof.get();
        }

        return null;
    }
}
