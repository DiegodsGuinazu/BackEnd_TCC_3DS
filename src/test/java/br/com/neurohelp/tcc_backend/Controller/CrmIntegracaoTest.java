package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Service.CrmService;
import br.com.neurohelp.tcc_backend.Service.RecuperacaoLimites;
import br.com.neurohelp.tcc_backend.Controller.Settings.CrmController;
import br.com.neurohelp.tcc_backend.Controller.Perfil.PerfilController;
import br.com.neurohelp.tcc_backend.DTO.AtualizarPerfilDTO;
import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import br.com.neurohelp.tcc_backend.Repository.profissionalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrmIntegracaoTest {
    @Test void falhaNoCrmNaoSalvaPerfilParcialmente() {
        var repository = mock(profissionalRepository.class);
        var crm = mock(CrmService.class);
        var usuario = new UserProf();
        usuario.setNome("Nome original");
        usuario.setNumRegistro("CRM-SP 12345");
        var dto = new AtualizarPerfilDTO("Outro Nome", "Bio nova", null, null, null, null, "CRM-SP 1234567");
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE))
                .when(crm).validarRegistroSeCrm("CRM-SP 1234567", "Outro Nome");
        var controller = new PerfilController(repository, crm, new RecuperacaoLimites());
        assertThrows(ResponseStatusException.class, () -> controller.atualizarPerfil(usuario, dto));
        assertEquals("Nome original", usuario.getNome());
        assertEquals("CRM-SP 12345", usuario.getNumRegistro());
        assertNull(usuario.getBio());
        verifyNoInteractions(repository);
    }

    @Test void editarSomenteBioNaoReconsultaUmCrmLegado() {
        var repository = mock(profissionalRepository.class);
        var crm = mock(CrmService.class);
        var usuario = new UserProf();
        usuario.setNome("Nome"); usuario.setNumRegistro("CRM-SP 12345");
        when(repository.save(usuario)).thenReturn(usuario);
        var dto = new AtualizarPerfilDTO(null, "Nova bio", null, null, null, null, null);
        new PerfilController(repository, crm, new RecuperacaoLimites()).atualizarPerfil(usuario, dto);
        verifyNoInteractions(crm);
        assertEquals("Nova bio", usuario.getBio());
    }

    @Test void consultaPublicaTemLimitePorInstancia() {
        var crm = mock(CrmService.class);
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        var controller = new CrmController(crm, new RecuperacaoLimites());
        var dto = new CrmController.Consulta("12345", "SP", "Nome");
        for (int i = 0; i < 20; i++) controller.consultar(dto, request);
        assertEquals(429, assertThrows(ResponseStatusException.class, () -> controller.consultar(dto, request)).getStatusCode().value());
        verify(crm, times(20)).consultar("12345", "SP", "Nome");
    }
}
