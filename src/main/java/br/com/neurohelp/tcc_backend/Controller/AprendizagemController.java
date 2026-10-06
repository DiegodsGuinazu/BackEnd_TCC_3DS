package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.DTO.AprendizagemResponseDTO;
import br.com.neurohelp.tcc_backend.DTO.AprendizagemResumoResponseDTO;
import br.com.neurohelp.tcc_backend.Service.AprendizagemService;
import br.com.neurohelp.tcc_backend.DTO.PostagemEdicaoDTO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
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

    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AprendizagemResponseDTO> criar(@Valid @RequestBody PostagemEdicaoDTO dto) {
        return ResponseEntity.status(201).body(service.criar(dto));
    }

    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public AprendizagemResponseDTO editar(@PathVariable Long id, @Valid @RequestBody PostagemEdicaoDTO dto) {
        return service.editar(id, dto);
    }

    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id); return ResponseEntity.noContent().build();
    }
}
