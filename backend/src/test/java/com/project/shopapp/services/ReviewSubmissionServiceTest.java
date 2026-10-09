package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.shopapp.dtos.CreateProductReviewRequest;
import com.project.shopapp.exceptions.DuplicateResourceException;
import com.project.shopapp.services.impl.ReviewSubmissionServiceImpl;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ReviewSubmissionServiceTest {
    @Mock MediaService media;
    @Mock ProductReviewService reviews;
    private final CreateProductReviewRequest request =
            new CreateProductReviewRequest(21L, 5, "Great");
    private final MockMultipartFile file =
            new MockMultipartFile("images", "photo.png", "image/png", new byte[] {1});

    @Test
    void rejectsTooManyPhotosBeforeStorage() throws IOException {
        var service = new ReviewSubmissionServiceImpl(media, reviews);
        assertThrows(
                IllegalArgumentException.class,
                () -> service.submit(8L, "buyer", request, Collections.nCopies(6, file)));
        verify(media, never()).upload(any());
    }

    @Test
    void duplicateReviewDoesNotUpload() throws IOException {
        when(reviews.reviewed(8L, "buyer", 21L, null)).thenReturn(true);
        var service = new ReviewSubmissionServiceImpl(media, reviews);
        assertThrows(
                DuplicateResourceException.class,
                () -> service.submit(8L, "buyer", request, List.of(file)));
        verify(media, never()).upload(any());
    }

    @Test
    void failedDatabaseWriteCleansOnlyNewUploads() throws IOException {
        String url = "/api/v1/media/new.png";
        when(media.upload(file)).thenReturn(url);
        when(reviews.createWithImages(8L, "buyer", request, List.of(url)))
                .thenThrow(new DuplicateResourceException("Concurrent review"));
        var service = new ReviewSubmissionServiceImpl(media, reviews);
        assertThrows(
                DuplicateResourceException.class,
                () -> service.submit(8L, "buyer", request, List.of(file)));
        verify(media).deleteUpload(url);
    }

    @Test
    void partialUploadFailureCleansEarlierFiles() throws IOException {
        when(media.upload(file))
                .thenReturn("/api/v1/media/new.png")
                .thenThrow(new IOException("Storage unavailable"));
        var service = new ReviewSubmissionServiceImpl(media, reviews);
        assertThrows(
                IOException.class, () -> service.submit(8L, "buyer", request, List.of(file, file)));
        verify(media).deleteUpload("/api/v1/media/new.png");
        verify(reviews, never()).createWithImages(any(), any(), any(), any());
    }
}
