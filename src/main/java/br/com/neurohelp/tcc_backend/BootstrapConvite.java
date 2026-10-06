package br.com.neurohelp.tcc_backend;

import br.com.neurohelp.tcc_backend.Service.ConviteAdminService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.List;
import java.util.EnumSet;

/** Comando interno: lê e-mail e destino do ambiente e nunca imprime o link. */
public class BootstrapConvite {
    @Configuration @Profile("bootstrap-convite")
    public static class Config {
        @Bean PasswordEncoder bootstrapPasswordEncoder() { return new BCryptPasswordEncoder(); }
    }
    public static void main(String[] args) throws Exception {
        String email = System.getenv("ADMIN_INVITE_EMAIL");
        String destino = System.getenv("ADMIN_INVITE_OUTPUT");
        if (email == null || destino == null || destino.isBlank()) {
            throw new IllegalArgumentException("Defina ADMIN_INVITE_EMAIL e ADMIN_INVITE_OUTPUT (arquivo novo em diretório privado).");
        }
        Path arquivo = Path.of(destino).toAbsolutePath();
        // Cria o arquivo antes do convite; nunca sobrescreve arquivo existente.
        if (Files.getFileStore(arquivo.getParent()).supportsFileAttributeView("posix")) {
            Files.createFile(arquivo, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        } else {
            Files.createFile(arquivo);
            AclFileAttributeView acl = Files.getFileAttributeView(arquivo, AclFileAttributeView.class);
            if (acl == null) throw new IllegalStateException("O sistema deve suportar permissões privadas de arquivo.");
            var operador = arquivo.getFileSystem().getUserPrincipalLookupService()
                    .lookupPrincipalByName(System.getProperty("user.name"));
            acl.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(operador)
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build()));
        }
        SpringApplication app = new SpringApplication(TccBackendApplication.class, Config.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setAdditionalProfiles("bootstrap-convite");
        try (var context = app.run()) {
            ConviteAdminService service = context.getBean(ConviteAdminService.class);
            var convite = service.gerarPrimeiro(email);
            try { Files.writeString(arquivo, convite.link() + System.lineSeparator()); }
            catch (Exception e) { service.revogar(convite.id()); throw e; }
            System.out.println("Primeiro convite gravado no arquivo privado indicado. Compartilhe por canal seguro.");
        }
    }
}
