package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateReviewRequest;
import com.ziyara.backend.application.dto.request.ModerateReviewRequest;
import com.ziyara.backend.application.dto.request.UpdateReviewRequest;
import com.ziyara.backend.application.dto.response.ReviewResponse;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.application.exception.UnauthorizedException;
import com.ziyara.backend.domain.entity.Review;
import com.ziyara.backend.domain.enums.ReviewStatus;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.ReviewRepository;
import com.ziyara.backend.modules.notification.api.StaffNotificationCommandPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock BookingRepository bookingRepository;
    @Mock StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    @InjectMocks ReviewService reviewService;

    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final UUID BOOKING_ID = UUID.randomUUID();

    private Review savedReview;

    @BeforeEach
    void setUp() {
        savedReview = new Review();
        savedReview.setId(REVIEW_ID);
        savedReview.setUserId(USER_ID);
        savedReview.setServiceId(SERVICE_ID);
        savedReview.setBookingId(BOOKING_ID);
        savedReview.setRating(4);
        savedReview.setComment("Great service");
        savedReview.setStatus(ReviewStatus.PENDING);
    }

    // â”€â”€ getReview â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Nested
    class GetReview {

        @Test
        void found_returnsResponse() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(savedReview));

            ReviewResponse response = reviewService.getReview(REVIEW_ID);

            assertThat(response.getId()).isEqualTo(REVIEW_ID);
            assertThat(response.getRating()).isEqualTo(4);
        }

        @Test
        void notFound_throwsResourceNotFound() {
            when(reviewRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.getReview(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // â”€â”€ getServiceReviews â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void getServiceReviews_returnsMappedList() {
        when(reviewRepository.findByServiceIdAndStatusIn(eq(SERVICE_ID), any()))
                .thenReturn(List.of(savedReview));

        List<ReviewResponse> result = reviewService.getServiceReviews(SERVICE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getServiceId()).isEqualTo(SERVICE_ID);
    }

    // â”€â”€ respondToReview â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void respondToReview_setsResponseAndPublished() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(savedReview));
        when(reviewRepository.save(any())).thenReturn(savedReview);

        ReviewResponse result = reviewService.respondToReview(REVIEW_ID, "Thank you!");

        assertThat(savedReview.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
        assertThat(savedReview.getResponse()).isEqualTo("Thank you!");
    }

    // â”€â”€ updateReview â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Nested
    class UpdateReview {

        @Test
        void author_canUpdate() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(savedReview));
            when(reviewRepository.save(any())).thenReturn(savedReview);

            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setRating(5);
            request.setComment("Updated comment");

            ReviewResponse result = reviewService.updateReview(REVIEW_ID, request, USER_ID, false);

            assertThat(savedReview.getRating()).isEqualTo(5);
            assertThat(savedReview.getComment()).isEqualTo("Updated comment");
        }

        @Test
        void nonAuthorNonStaff_throwsUnauthorized() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(savedReview));

            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setRating(1);

            assertThatThrownBy(() -> reviewService.updateReview(REVIEW_ID, request, OTHER_USER_ID, false))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void staffUser_canUpdateAnyReview() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(savedReview));
            when(reviewRepository.save(any())).thenReturn(savedReview);

            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setComment("Staff edit");

            ReviewResponse result = reviewService.updateReview(REVIEW_ID, request, OTHER_USER_ID, true);

            assertThat(savedReview.getComment()).isEqualTo("Staff edit");
        }

        @Test
        void notFound_throwsResourceNotFound() {
            when(reviewRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.updateReview(UUID.randomUUID(), new UpdateReviewRequest(), USER_ID, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // â”€â”€ deleteReview â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Nested
    class DeleteReview {

        @Test
        void author_canDelete() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(savedReview));

            reviewService.deleteReview(REVIEW_ID, USER_ID, false);

            verify(reviewRepository).deleteById(REVIEW_ID);
        }

        @Test
        void nonAuthorNonStaff_throwsUnauthorized() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(savedReview));

            assertThatThrownBy(() -> reviewService.deleteReview(REVIEW_ID, OTHER_USER_ID, false))
                    .isInstanceOf(UnauthorizedException.class);
            verify(reviewRepository, never()).deleteById(any());
        }

        @Test
        void staffUser_canDeleteAny() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(savedReview));

            reviewService.deleteReview(REVIEW_ID, OTHER_USER_ID, true);

            verify(reviewRepository).deleteById(REVIEW_ID);
        }
    }

    // â”€â”€ moderateReview â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Nested
    class ModerateReview {

        @Test
        void approve_setsApprovedStatus() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(savedReview));
            when(reviewRepository.save(any())).thenReturn(savedReview);

            ModerateReviewRequest request = new ModerateReviewRequest();
            request.setStatus(ReviewStatus.APPROVED);

            ReviewResponse result = reviewService.moderateReview(REVIEW_ID, request);

            assertThat(savedReview.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        }

        @Test
        void reject_setsRejectedStatus() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(savedReview));
            when(reviewRepository.save(any())).thenReturn(savedReview);

            ModerateReviewRequest request = new ModerateReviewRequest();
            request.setStatus(ReviewStatus.REJECTED);
            request.setRejectionReason("Spam content");

            ReviewResponse result = reviewService.moderateReview(REVIEW_ID, request);

            assertThat(savedReview.getStatus()).isEqualTo(ReviewStatus.REJECTED);
        }
    }
}

