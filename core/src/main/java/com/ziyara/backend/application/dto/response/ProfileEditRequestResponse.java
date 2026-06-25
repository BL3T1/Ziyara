package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.entity.ProviderProfileEditRequest;
import com.ziyara.backend.domain.enums.EditRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileEditRequestResponse {
    private UUID id;
    private UUID providerId;
    private UUID requestedBy;
    private EditRequestStatus status;
    private Map<String, Object> diffJson;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;

    public static ProfileEditRequestResponse from(ProviderProfileEditRequest req) {
        return ProfileEditRequestResponse.builder()
                .id(req.getId())
                .providerId(req.getProviderId())
                .requestedBy(req.getRequestedBy())
                .status(req.getStatus())
                .diffJson(req.getDiffJson())
                .reviewedBy(req.getReviewedBy())
                .reviewedAt(req.getReviewedAt())
                .rejectionReason(req.getRejectionReason())
                .createdAt(req.getCreatedAt())
                .build();
    }
}
