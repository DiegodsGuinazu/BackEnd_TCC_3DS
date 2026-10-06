package br.com.neurohelp.tcc_backend.Service;

import br.com.neurohelp.tcc_backend.DTO.ProfissionalResponseDTO;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProfissionalService {
    private final profissionalRepository repository;

    public ProfissionalService(profissionalRepository repository) {
        this.repository = repository;
    }

    public List<ProfissionalResponseDTO> listar() {
        return repository.findAll(Sort.by("nome").ascending().and(Sort.by("id")))
                .stream().map(ProfissionalResponseDTO::new).toList();
    }

    public ProfissionalResponseDTO buscar(Long id) {
        return repository.findById(id).map(ProfissionalResponseDTO::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional não encontrado."));
    }

    public byte[] foto(Long id) {
        String foto = repository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Profissional não encontrado.")).getFotoPerfil();
        if (foto == null || !foto.startsWith("data:image/jpeg;base64,")) throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Foto não disponível.");
        return java.util.Base64.getDecoder().decode(foto.substring(foto.indexOf(',') + 1));
    }
}
