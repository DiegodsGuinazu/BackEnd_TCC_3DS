package br.com.neurohelp.tcc_backend.Service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;

@Service
public class ImagemPostagemService {
    // Limita os bytes e as dimensões antes de decodificar e remove metadados ao reencodificar.
    public String normalizar(String data) {
        if (data == null || data.length() > 2800000 ||
                !(data.startsWith("data:image/jpeg;base64,") || data.startsWith("data:image/png;base64,"))) throw invalida();
        try {
            byte[] bytes = Base64.getDecoder().decode(data.substring(data.indexOf(',') + 1));
            if (bytes.length == 0 || bytes.length > 2 * 1024 * 1024) throw invalida();
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalida();
                var reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    String format = reader.getFormatName();
                    if (!(format.equalsIgnoreCase("JPEG") || format.equalsIgnoreCase("PNG"))
                            || format.equalsIgnoreCase("JPEG") != data.startsWith("data:image/jpeg;base64,")) throw invalida();
                    int w = reader.getWidth(0), h = reader.getHeight(0);
                    if (w < 1 || h < 1 || w > 4096 || h > 4096 || (long) w * h > 12000000) throw invalida();
                    var original = reader.read(0);
                    double scale = Math.min(1.0, 1600.0 / Math.max(w, h));
                    var result = new BufferedImage(Math.max(1, (int)(w * scale)), Math.max(1, (int)(h * scale)), BufferedImage.TYPE_INT_RGB);
                    var g = result.createGraphics();
                    try {
                        g.setColor(Color.WHITE); g.fillRect(0, 0, result.getWidth(), result.getHeight());
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g.drawImage(original, 0, 0, result.getWidth(), result.getHeight(), null);
                    } finally { g.dispose(); }
                    var output = new ByteArrayOutputStream();
                    if (!ImageIO.write(result, "jpeg", output)) throw invalida();
                    return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
                } finally { reader.dispose(); }
            }
        } catch (ResponseStatusException e) { throw e; }
        catch (Exception e) { throw invalida(); }
    }
    private ResponseStatusException invalida() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Envie JPEG ou PNG de até 2 MB, 4096 pixels por lado e 12 megapixels.");
    }
}
