package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.Entity.User.ConfirmacaoEmail;
import jakarta.persistence.EntityManager;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

/** Dados pré-confirmados somente para testes de cadastro que tratam de outros requisitos. */
final class ConfirmacaoEmailFixture {
    static void criar(EntityManager em, String segredo, String email) {
        try {
            var c = new ConfirmacaoEmail();
            c.setEmailHash(hash(segredo, email)); c.setCodigoHash(hash(segredo, email + ":12345678"));
            c.setEnviadoEm(Instant.now()); c.setExpiraEm(Instant.now().plusSeconds(900));
            em.persist(c);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static String hash(String segredo, String valor) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(("confirmacao-email:" + valor).getBytes(StandardCharsets.UTF_8)));
    }
}
