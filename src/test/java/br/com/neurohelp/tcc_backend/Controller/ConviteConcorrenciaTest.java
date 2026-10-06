package br.com.neurohelp.tcc_backend.Controller;

import br.com.neurohelp.tcc_backend.DTO.CadastroAdminDTO;
import br.com.neurohelp.tcc_backend.Repository.*;
import br.com.neurohelp.tcc_backend.Service.ConviteAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConviteConcorrenciaTest {
    @Autowired ConviteAdminService service;
    @Autowired AdminRepository admins;
    @Autowired ConviteAdminRepository convites;

    @Test void somenteUmaTransacaoPodeConsumirOMesmoConvite() throws Exception {
        String email = "concorrencia-" + UUID.randomUUID() + "@teste.invalid";
        var convite = service.gerar(email);
        String token = convite.link().split("#token=")[1];
        CountDownLatch pronto = new CountDownLatch(2), iniciar = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> cadastro = () -> {
                pronto.countDown(); iniciar.await();
                try { service.cadastrar(new CadastroAdminDTO("Teste", email, "Senha@123", token)); return true; }
                catch (org.springframework.web.server.ResponseStatusException e) {
                    assertEquals(400, e.getStatusCode().value()); return false;
                }
            };
            Future<Boolean> a = pool.submit(cadastro), b = pool.submit(cadastro);
            assertTrue(pronto.await(10, TimeUnit.SECONDS)); iniciar.countDown();
            assertNotEquals(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertTrue(admins.findByEmailIgnoreCase(email).isPresent());
            assertNotNull(convites.findById(convite.id()).orElseThrow().getUtilizadoEm());
        } finally {
            // Remove apenas registros exclusivos criados por este teste, nunca dados preexistentes.
            admins.findByEmailIgnoreCase(email).ifPresent(admins::delete);
            convites.deleteById(convite.id());
        }
    }
}
