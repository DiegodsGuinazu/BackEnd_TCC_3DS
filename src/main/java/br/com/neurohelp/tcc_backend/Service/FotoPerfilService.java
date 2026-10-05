package br.com.neurohelp.tcc_backend.Service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Iterator;

@Service
public class FotoPerfilService {
    public String normalizar(String dataUrl) {
        if (dataUrl == null || dataUrl.length() > 180000 ||
                !(dataUrl.startsWith("data:image/jpeg;base64,") || dataUrl.startsWith("data:image/png;base64,"))) {
            throw invalida();
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1));
            if (bytes.length == 0 || bytes.length > 128 * 1024) throw invalida();
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalida();
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    String format = reader.getFormatName();
                    if (!(format.equalsIgnoreCase("JPEG") || format.equalsIgnoreCase("PNG"))) throw invalida();
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > 1024 || height > 1024) throw invalida();
                    BufferedImage original = reader.read(0);
                    BufferedImage avatar = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = avatar.createGraphics();
                    try {
                        g.setColor(Color.WHITE);
                        g.fillRect(0, 0, 256, 256);
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        int size = Math.min(width, height), x = (width - size) / 2, y = (height - size) / 2;
                        g.drawImage(original, 0, 0, 256, 256, x, y, x + size, y + size, null);
                    } finally { g.dispose(); }
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    if (!ImageIO.write(avatar, "jpeg", output)) throw invalida();
                    // Reencodifica e remove metadados; não armazena o arquivo enviado diretamente.
                    return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
                } finally { reader.dispose(); }
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw invalida();
        }
    }

    private ResponseStatusException invalida() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Envie uma imagem JPEG ou PNG de até 128 KB e 1024 x 1024 pixels.");
    }
}
