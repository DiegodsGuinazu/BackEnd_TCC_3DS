package br.com.neurohelp.tcc_backend.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Consulta pontual ao WS oficial, sem divulgar chave ou dados do médico ao cliente. */
@Service
public class CrmService {
    private static final URI ENDPOINT = URI.create("https://ws.cfm.org.br:8080/WebServiceConsultaMedicos/ServicoConsultaMedicos");
    private static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");
    // Forma canônica armazenada no campo existente. P é inscrição provisória.
    private static final Pattern REGISTRO = Pattern.compile("^CRM[-/ ]([A-Z]{2})\\s+(\\d{1,7})(-P)?$", Pattern.CASE_INSENSITIVE);
    private final String chave;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public CrmService(@Value("${app.crm.cfm-chave:}") String chave) { this.chave = chave.trim(); }

    public static class ConsultaException extends ResponseStatusException {
        ConsultaException(HttpStatus status, String reason) { super(status, reason); }
    }

    public record Resultado(Boolean valido, boolean cadastroConsultado, String status, String mensagem) {}

    public Resultado consultar(String crm, String uf, String nome) {
        validarEntrada(crm, uf, nome);
        if (chave.isBlank()) throw indisponivel("Consulta ao CFM ainda não configurada. Não foi possível confirmar o CRM.");
        String xml = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://servico.cfm.org.br/\"><soapenv:Body><ser:Consultar><crm>"
                + crm + "</crm><uf>" + uf.toUpperCase(Locale.ROOT) + "</uf><chave>" + escape(chave)
                + "</chave></ser:Consultar></soapenv:Body></soapenv:Envelope>";
        try {
            var request = HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "text/xml; charset=utf-8").header("SOAPAction", "\"\"")
                    .POST(HttpRequest.BodyPublishers.ofString(xml)).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var body = response.body()) {
                if (response.statusCode() != 200) throw indisponivel();
                byte[] bytes = body.readNBytes(262145);
                if (bytes.length > 262144) throw indisponivel();
                return interpretar(new String(bytes, StandardCharsets.UTF_8), crm, uf, nome);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw indisponivel();
        } catch (ResponseStatusException e) { throw e; }
        catch (Exception e) { throw indisponivel(); }
    }

    /** Não altera registros de outros conselhos nem transforma números legados em CRM. */
    public void validarRegistroSeCrm(String registro, String nome) {
        if (registro == null || !registro.stripLeading().toUpperCase(Locale.ROOT).startsWith("CRM")) return;
        var match = REGISTRO.matcher(registro.trim());
        if (!match.matches()) throw new ConsultaException(HttpStatus.BAD_REQUEST,
                "Informe o CRM no formato CRM-SP 1234567, com UF e até 7 dígitos (sufixo -P se provisório).");
        Resultado resultado = consultar(match.group(2), match.group(1), nome);
        if (!Boolean.TRUE.equals(resultado.valido())) throw new ConsultaException(HttpStatus.UNPROCESSABLE_ENTITY, resultado.mensagem());
    }

    static void validarEntrada(String crm, String uf, String nome) {
        if (crm == null || !crm.matches("\\d{1,7}") || crm.matches("0+") || uf == null
                || !UFS.contains(uf.toUpperCase(Locale.ROOT)) || nome == null || nome.isBlank() || nome.length() > 120)
            throw new ConsultaException(HttpStatus.BAD_REQUEST, "Informe nome, UF do CRM e número de 1 a 7 dígitos, diferente de zero.");
    }

    // Visível no pacote para testes com envelopes locais; nenhum teste depende do CFM.
    static Resultado interpretar(String xml, String crm, String uf, String nome) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            String codigo = text(doc, "codigoErro");
            if ("8101".equals(codigo)) return new Resultado(false, true, "NAO_ENCONTRADO", "CRM não encontrado no CFM para a UF informada. Confira os dados.");
            if (!codigo.isEmpty()) throw indisponivel();
            String returnedCrm = text(doc, "crm").replaceFirst("^300-EMFE-", "").replaceFirst("-?P$", "");
            if (!returnedCrm.matches("\\d{1,7}") || Integer.parseInt(returnedCrm) != Integer.parseInt(crm)
                    || !uf.equalsIgnoreCase(text(doc, "uf")) || text(doc, "nome").isBlank() || text(doc, "situacao").isBlank()) throw indisponivel();
            if (!"A".equals(text(doc, "situacao"))) return new Resultado(false, true, "NAO_REGULAR", "O CRM foi localizado, mas a inscrição não está regular no CFM.");
            if (!normalizarNome(nome).equals(normalizarNome(text(doc, "nome")))) return new Resultado(false, true, "NOME_DIVERGENTE", "O nome informado não corresponde ao cadastro deste CRM no CFM.");
            return new Resultado(true, true, "REGULAR", "CRM localizado, regular e com nome correspondente no CFM. Esta consulta não comprova a identidade de quem preenche o formulário.");
        } catch (ResponseStatusException e) { throw e; }
        catch (Exception e) { throw indisponivel(); }
    }

    private static String text(Document doc, String name) {
        var nodes = doc.getElementsByTagNameNS("*", name);
        return nodes.getLength() == 1 ? nodes.item(0).getTextContent().trim() : "";
    }
    private static String normalizarNome(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
    private static String escape(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;"); }
    private static ResponseStatusException indisponivel() { return indisponivel("Não foi possível consultar o CFM agora. Tente novamente mais tarde; o CRM não foi classificado como falso."); }
    private static ResponseStatusException indisponivel(String message) { return new ConsultaException(HttpStatus.SERVICE_UNAVAILABLE, message); }
}
