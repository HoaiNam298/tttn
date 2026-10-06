package com.project.shopapp.services.impl;

import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.services.MediaService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaServiceImpl implements MediaService {
    private final Path directory;

    public MediaServiceImpl(@Value("${app.media.directory:uploads}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public String upload(MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Image must be non-empty and at most 5 MB");
        }
        try (var input = file.getInputStream();
                var stream = ImageIO.createImageInputStream(input)) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Only valid JPEG and PNG images are supported");
            }
            var reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("png")) {
                    throw new IllegalArgumentException("Only JPEG and PNG images are supported");
                }
                reader.setInput(stream);
                java.awt.image.BufferedImage image;
                try {
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width < 1 || height < 1 || (long) width * height > 16000000) {
                        throw new IllegalArgumentException("Image exceeds the 16 megapixel limit");
                    }
                    image = reader.read(0);
                } catch (javax.imageio.IIOException exception) {
                    throw new IllegalArgumentException("Invalid image data", exception);
                }
                String extension = format.equals("jpeg") ? "jpg" : "png";
                String filename = UUID.randomUUID() + "." + extension;
                Files.createDirectories(directory);
                Path target = directory.resolve(filename);
                try {
                    if (!ImageIO.write(image, format, target.toFile())) {
                        throw new IOException("Image encoder unavailable");
                    }
                } catch (IOException exception) {
                    Files.deleteIfExists(target);
                    throw exception;
                }
                return "/api/v1/media/" + filename;
            } finally {
                reader.dispose();
            }
        }
    }

    @Override
    public Resource read(String filename) {
        if (!filename.matches("^[a-f0-9-]{36}\\.(jpg|png)$")) {
            throw new ResourceNotFoundException("Image not found");
        }
        Path file = directory.resolve(filename).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Image not found");
        }
        return new FileSystemResource(file);
    }
}
