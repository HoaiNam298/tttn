package com.project.shopapp.services;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {
    String upload(MultipartFile file) throws IOException;

    Resource read(String filename);
}
