package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.DTO.ProfissionalResponseDTO;
import br.com.neurohelp.tcc_backend.Service.ProfissionalService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.util.List;

@RestController
@RequestMapping("/api/profissionais")
public class ProfissionaisController {
    private final ProfissionalService service;

    public ProfissionaisController(ProfissionalService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ProfissionalResponseDTO>> listar() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfissionalResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.buscar(id));
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG)
                .header("X-Content-Type-Options", "nosniff").body(service.foto(id));
    }
}
