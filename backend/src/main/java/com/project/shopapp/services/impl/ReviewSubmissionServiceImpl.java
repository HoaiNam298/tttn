package com.project.shopapp.services.impl;

import com.project.shopapp.dtos.CreateProductReviewRequest;
import com.project.shopapp.exceptions.DuplicateResourceException;
import com.project.shopapp.responses.ProductReviewResponse;
import com.project.shopapp.services.MediaService;
import com.project.shopapp.services.ProductReviewService;
import com.project.shopapp.services.ReviewSubmissionService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReviewSubmissionServiceImpl implements ReviewSubmissionService {
    private final MediaService media;
    private final ProductReviewService reviews;

    public ReviewSubmissionServiceImpl(MediaService media, ProductReviewService reviews) {
        this.media = media;
        this.reviews = reviews;
    }

    @Override
    public ProductReviewResponse submit(
            Long productId,
            String phoneNumber,
            CreateProductReviewRequest request,
            List<MultipartFile> files)
            throws IOException {
        if (files.size() > 5) {
            throw new IllegalArgumentException("At most five review images are allowed");
        }
        if (reviews.reviewed(productId, phoneNumber, request.orderId(), request.variantId())) {
            throw new DuplicateResourceException("This order item has already been reviewed");
        }
        List<String> urls = new ArrayList<>();
        try {
            // File decoding/storage happens outside the DB transaction.
            for (MultipartFile file : files) {
                urls.add(media.upload(file));
            }
            return reviews.createWithImages(productId, phoneNumber, request, urls);
        } catch (IOException | RuntimeException exception) {
            for (String url : urls) {
                try {
                    media.deleteUpload(url);
                } catch (IOException | RuntimeException cleanupException) {
                    exception.addSuppressed(cleanupException);
                    org.slf4j.LoggerFactory.getLogger(ReviewSubmissionServiceImpl.class)
                            .error(
                                    "Failed to clean up uncommitted review image {}",
                                    url,
                                    cleanupException);
                }
            }
            throw exception;
        }
    }
}
