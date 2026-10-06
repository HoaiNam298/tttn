package com.project.shopapp.controller;

import com.project.shopapp.responses.ImageUploadResponse;
import com.project.shopapp.services.MediaService;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MediaController {
    private final MediaService media;

    public MediaController(MediaService media) {
        this.media = media;
    }

    @PostMapping(value = "/api/v1/admin/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> upload(@RequestParam MultipartFile file)
            throws IOException {
        String url = media.upload(file);
        return ResponseEntity.created(URI.create(url)).body(new ImageUploadResponse(url));
    }

    @GetMapping("/api/v1/media/{filename}")
    public ResponseEntity<Resource> read(@PathVariable String filename) {
        Resource resource = media.read(filename);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic().immutable())
                .contentType(filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                .body(resource);
    }
}
