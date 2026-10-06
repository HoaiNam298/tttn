package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.services.impl.MediaServiceImpl;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class MediaServiceTest {
    @TempDir Path directory;

    @Test
    void storesCanonicalImageWithGeneratedNameAndServesIt() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB), "png", output);
        var service = new MediaServiceImpl(directory.toString());
        String url =
                service.upload(
                        new MockMultipartFile(
                                "file", "../../evil.svg", "text/plain", output.toByteArray()));
        assertTrue(url.matches("/api/v1/media/[a-f0-9-]{36}\\.png"));
        var resource = service.read(url.substring(url.lastIndexOf('/') + 1));
        try (var input = resource.getInputStream()) {
            assertEquals(3, ImageIO.read(input).getHeight());
        }
    }

    @Test
    void rejectsNonImageAndTraversal() {
        var service = new MediaServiceImpl(directory.toString());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.upload(
                                new MockMultipartFile(
                                        "file",
                                        "image.png",
                                        "image/png",
                                        "<script>alert(1)</script>"
                                                .getBytes(
                                                        java.nio.charset.StandardCharsets.UTF_8))));
        assertThrows(ResourceNotFoundException.class, () -> service.read("../secret.png"));
    }
}
