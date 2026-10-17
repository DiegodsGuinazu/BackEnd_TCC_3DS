package br.com.neurohelp.tcc_backend.Service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class ImagemPostagemServiceTest {
    final ImagemPostagemService service = new ImagemPostagemService();
    String png(int width, int height) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", output);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    }
    @Test void reduzMantendoProporcaoSemCorte() throws Exception {
        String result = service.normalizar(png(3200, 1600));
        assertTrue(result.startsWith("data:image/jpeg;base64,"));
        var image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(result.split(",")[1])));
        assertEquals(1600, image.getWidth()); assertEquals(800, image.getHeight());
    }
    @Test void recusaDimensoesAbusivasFormatoFalsoSvgEBytesEmExcesso() throws Exception {
        for (String data : new String[] {png(4097, 1), png(4000, 3100),
                png(2, 2).replace("image/png", "image/jpeg"), "data:image/svg+xml;base64,PHN2Zz4=",
                "data:image/png;base64," + "A".repeat(2800000)}) {
            assertThrows(ResponseStatusException.class, () -> service.normalizar(data));
        }
    }
}
