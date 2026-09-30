package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.DTO.AprendizagemResponseDTO;
import br.com.neurohelp.tcc_backend.DTO.AprendizagemResumoResponseDTO;
import br.com.neurohelp.tcc_backend.Service.AprendizagemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aprendizagem")
public class AprendizagemController {
    private final AprendizagemService service;

    public AprendizagemController(AprendizagemService service) {
        this.service = service;
    }

    @GetMapping
    public List<AprendizagemResumoResponseDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public AprendizagemResponseDTO buscar(@PathVariable Long id) {
        return service.buscar(id);
    }
}
