package com.project.shopapp.services;

import com.project.shopapp.dtos.request.CreateProductReviewRequest;
import com.project.shopapp.dtos.response.ProductReviewResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ReviewSubmissionService {
    ProductReviewResponse submit(
            Long productId,
            String phoneNumber,
            CreateProductReviewRequest request,
            List<MultipartFile> files)
            throws IOException;
}
