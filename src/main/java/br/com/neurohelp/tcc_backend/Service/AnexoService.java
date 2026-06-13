package br.com.neurohelp.tcc_backend.Service;

import br.com.neurohelp.tcc_backend.Entity.Postagem.Anexo.Anexo;
import br.com.neurohelp.tcc_backend.Repository.AnexoRepository;


import java.util.Optional;

public class AnexoService {
    private static AnexoRepository anexoRepository;

    public AnexoService(AnexoRepository anexoRepository) {
        AnexoService.anexoRepository = anexoRepository;
    }

    //ID
    public Optional<Anexo> findById(Long id) {
        anexoRepository.findById(id);
        return Optional.empty();
    }

    public static Anexo salvar(Anexo anexo){
        return anexoRepository.save(anexo);
    }

}
