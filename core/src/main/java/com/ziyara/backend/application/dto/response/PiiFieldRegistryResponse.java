package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "PII field registry entry — column-level data classification")
public class PiiFieldRegistryResponse {

    private UUID id;
    private String tableName;
    private String columnName;
    private String piiCategory;
    private Boolean encryptionRequired;
    private String gdprArticle;
    private LocalDateTime lastReviewedAt;
}
