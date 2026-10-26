package br.com.neurohelp.tcc_backend.Service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class CrmServiceTest {
    private static String resposta(String crm, String uf, String nome, String situacao) {
        return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
                + "<ConsultarResponse><dadosMedico><crm>" + crm + "</crm><uf>" + uf + "</uf><nome>" + nome
                + "</nome><situacao>" + situacao + "</situacao></dadosMedico></ConsultarResponse></soap:Body></soap:Envelope>";
    }

    @Test void aceitaSeteDigitosEUfReal() {
        assertDoesNotThrow(() -> CrmService.validarEntrada("1234567", "sp", "João Exemplo"));
        assertDoesNotThrow(() -> CrmService.validarEntrada("1", "SP", "João Exemplo"));
        for (String numero : new String[]{"12345678", "0", "0000000", "123A", "-123", "1 23", ""})
            assertEquals(400, assertThrows(ResponseStatusException.class,
                    () -> CrmService.validarEntrada(numero, "SP", "João Exemplo")).getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> CrmService.validarEntrada("123", "XX", "João"));
    }

    @Test void consultaSemChaveNaoClassificaRegistroComoFalso() {
        var erro = assertThrows(ResponseStatusException.class, () -> new CrmService("").consultar("123", "SP", "João"));
        assertEquals(503, erro.getStatusCode().value());
        assertTrue(erro.getReason().contains("não configurada"));
    }

    @Test void confirmaNumeroUfNomeESituacaoComAcentos() {
        var result = CrmService.interpretar(resposta("1234567", "SP", "João Exemplo", "A"), "1234567", "SP", " JOAO  EXEMPLO ");
        assertTrue(result.valido());
        assertTrue(result.cadastroConsultado());
        assertEquals("REGULAR", result.status());
    }

    @Test void preservaPrefixoESufixoDaRespostaSemConfundirNumero() {
        for (String numero : new String[]{"1234567-P", "300-EMFE-1234567", "1234567"})
            assertTrue(CrmService.interpretar(resposta(numero, "SP", "João", "A"), "1234567", "SP", "João").valido());
    }

    @Test void naoEncontradoEUmResultadoDaConsulta() {
        var result = CrmService.interpretar("<dadosMedico><codigoErro>8101</codigoErro></dadosMedico>", "123", "SP", "João");
        assertFalse(result.valido());
        assertTrue(result.cadastroConsultado());
        assertEquals("NAO_ENCONTRADO", result.status());
    }

    @Test void distingueInscricaoIrregularDeNomeDivergente() {
        assertEquals("NAO_REGULAR", CrmService.interpretar(resposta("123", "SP", "João", "C"), "123", "SP", "João").status());
        assertEquals("NOME_DIVERGENTE", CrmService.interpretar(resposta("123", "SP", "Outra Pessoa", "A"), "123", "SP", "João").status());
    }

    @Test void errosDeChaveTransporteOuRespostaIncompletaNaoSaoCrmFalso() {
        for (String xml : new String[]{"<dadosMedico><codigoErro>3010</codigoErro></dadosMedico>",
                "<dadosMedico><codigoErro>2040</codigoErro></dadosMedico>", "<Envelope/>", "XML inválido",
                resposta("999", "SP", "João", "A"), resposta("123", "RJ", "João", "A")})
            assertEquals(503, assertThrows(ResponseStatusException.class,
                    () -> CrmService.interpretar(xml, "123", "SP", "João")).getStatusCode().value());
    }

    @Test void bloqueiaEntidadesExternas() {
        String xml = "<!DOCTYPE x [<!ENTITY secret SYSTEM 'file:///etc/passwd'>]><dadosMedico><nome>&secret;</nome></dadosMedico>";
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> CrmService.interpretar(xml, "123", "SP", "João")).getStatusCode().value());
    }

    @Test void outrosConselhosENumerosLegadosNaoSaoReinterpretadosComoCrm() {
        var crm = new CrmService("");
        for (String registro : new String[]{"CRP 06/123456", "CRN 12345", "123456", ""})
            assertDoesNotThrow(() -> crm.validarRegistroSeCrm(registro, "Nome"));
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> crm.validarRegistroSeCrm("CRM-SP 12345678", "Nome")).getStatusCode().value());
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> crm.validarRegistroSeCrm("CRM-SP 1234567-P", "Nome")).getStatusCode().value());
    }
}
