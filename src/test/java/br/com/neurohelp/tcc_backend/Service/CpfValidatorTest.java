package br.com.neurohelp.tcc_backend.Service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CpfValidatorTest {
    @Test void aceitaDigitosCorretosComOuSemMascara() {
        assertTrue(CpfValidator.valido("529.982.247-25"));
        assertTrue(CpfValidator.valido("11144477735"));
        assertTrue(CpfValidator.valido("12345678909"));
    }
    @Test void rejeitaRepetidosComprimentosLetrasEDigitosAlterados() {
        for (String cpf : new String[]{"00000000000", "11111111111", "52998224724", "52998224715",
                "5299822472", "529982247255", "abc52998224725", "", "５２９９８２２４７２５"}) assertFalse(CpfValidator.valido(cpf));
        assertFalse(CpfValidator.valido(null));
    }
}
