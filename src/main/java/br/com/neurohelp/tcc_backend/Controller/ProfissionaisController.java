package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.DTO.ProfissionalResponseDTO;
import br.com.neurohelp.tcc_backend.Service.ProfissionalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profissionais")
public class ProfissionaisController {
    private final ProfissionalService service;

    public ProfissionaisController(ProfissionalService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProfissionalResponseDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ProfissionalResponseDTO buscar(@PathVariable Long id) {
        return service.buscar(id);
    }
}
