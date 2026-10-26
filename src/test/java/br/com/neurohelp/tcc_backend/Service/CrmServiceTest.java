package br.com.neurohelp.tcc_backend.Service;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class CrmServiceTest {
    @Test void formatoNaoConfirmaExistenciaENaoExigeChave() {
        var crm = new CrmService();
        assertDoesNotThrow(() -> crm.validarRegistroSeCrm("CRM-SP 1234567-P", "Nome completo"));
        var resultado = crm.consultar("1234567", "SP", "Nome completo");
        assertNull(resultado.valido()); assertFalse(resultado.cadastroConsultado());
        assertEquals("VALIDACAO_MANUAL", resultado.status());
    }
    @Test void rejeitaFormatoInvalidoMasPreservaOutrosConselhos() {
        var crm = new CrmService();
        for (String r : new String[]{"CRM-SP 0", "CRM-XX 123", "CRM-SP 12345678", "CRM 123A"})
            assertEquals(400, assertThrows(ResponseStatusException.class, () -> crm.validarRegistroSeCrm(r, "Nome")).getStatusCode().value());
        for (String r : new String[]{"CRP 06/123456", "CRN 12345", "123456", ""})
            assertDoesNotThrow(() -> crm.validarRegistroSeCrm(r, "Nome"));
    }
    @Test void legadoComecaPendenteEConselhoDiferenteNaoViraCrm() {
        var p = new UserProf(); p.setNome("Nome"); p.setNumRegistro("CRM-SP 123");
        assertEquals("PENDENTE", CrmService.verificacao(p).status());
        p.setNumRegistro("CRP 06/12345"); assertEquals("NAO_APLICAVEL", CrmService.verificacao(p).status());
    }
    @Test void seloExigeConferenciaDosMesmosDadosComDataERevisor() {
        var p = new UserProf(); p.setNome("Nome"); p.setNumRegistro("CRM-SP 123");
        p.setCrmNomeConferido("Nome"); p.setCrmRegistroConferido("CRM-SP 123"); p.setCrmStatus("VERIFICADO");
        assertEquals("PENDENTE", CrmService.verificacao(p).status());
        p.setCrmConferidoEm(Instant.now()); p.setCrmRevisorId(1L);
        assertEquals("VERIFICADO", CrmService.verificacao(p).status());
        p.setNome("Outro nome"); assertEquals("PENDENTE", CrmService.verificacao(p).status());
        assertNull(CrmService.verificacao(p).conferidoEm());
    }
}
