package br.com.neurohelp.tcc_backend.Service;

import br.com.neurohelp.tcc_backend.Entity.User.UserProf;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Formato local e estado da conferência manual. Nenhuma chamada ao serviço pago. */
@Service
public class CrmService {
    public static final String FONTE = "https://portal.cfm.org.br/busca-medicos/";
    private static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");
    private static final Pattern REGISTRO = Pattern.compile("^CRM[-/ ]([A-Z]{2})\\s+(\\d{1,7})(-P)?$", Pattern.CASE_INSENSITIVE);
    public static class ConsultaException extends ResponseStatusException {
        public ConsultaException(HttpStatus status, String reason) { super(status, reason); }
    }
    // Compatibilidade da rota antiga: formato aceito nunca significa autenticidade.
    public record Resultado(Boolean valido, boolean cadastroConsultado, String status, String mensagem) {}
    public record Verificacao(String status, Instant conferidoEm, String fonte) {}
    public Resultado consultar(String numero, String uf, String nome) {
        validarEntrada(numero, uf, nome);
        return new Resultado(null, false, "VALIDACAO_MANUAL",
                "Formato aceito. Após o cadastro, solicite a conferência gratuita na sua área. O CRM ainda não foi verificado.");
    }
    public void validarRegistroSeCrm(String registro, String nome) {
        if (!eCrm(registro)) return;
        var match = REGISTRO.matcher(registro.trim());
        if (!match.matches()) throw new ConsultaException(HttpStatus.BAD_REQUEST,
                "Informe CRM-SP 1234567: UF e número de até 7 dígitos (sufixo -P se provisório).");
        validarEntrada(match.group(2), match.group(1), nome);
    }
    static void validarEntrada(String numero, String uf, String nome) {
        if (numero == null || !numero.matches("\\d{1,7}") || numero.matches("0+") || uf == null
                || !UFS.contains(uf.toUpperCase(Locale.ROOT)) || nome == null || nome.isBlank() || nome.length() > 120)
            throw new ConsultaException(HttpStatus.BAD_REQUEST, "Informe nome completo, UF e número de 1 a 7 dígitos, diferente de zero.");
    }
    public static boolean eCrm(String registro) {
        return registro != null && registro.stripLeading().toUpperCase(Locale.ROOT).startsWith("CRM");
    }
    public static boolean dadosCorrespondem(UserProf p) {
        return Objects.equals(p.getNome(), p.getCrmNomeConferido()) && Objects.equals(p.getNumRegistro(), p.getCrmRegistroConferido());
    }
    public static Verificacao verificacao(UserProf p) {
        if (!eCrm(p.getNumRegistro())) return new Verificacao("NAO_APLICAVEL", null, null);
        if (!dadosCorrespondem(p)) return new Verificacao("PENDENTE", null, FONTE);
        String status = p.getCrmStatus();
        boolean conferido = "VERIFICADO".equals(status) && p.getCrmConferidoEm() != null && p.getCrmRevisorId() != null;
        if (!conferido && !"EM_ANALISE".equals(status) && !"NAO_CONFIRMADO".equals(status)) status = "PENDENTE";
        return new Verificacao(status, conferido ? p.getCrmConferidoEm() : null, FONTE);
    }
}
