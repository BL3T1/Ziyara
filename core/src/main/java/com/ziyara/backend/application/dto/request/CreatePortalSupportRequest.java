package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Submit a support request from the provider portal")
public class CreatePortalSupportRequest {

    @NotBlank
    @Size(max = 500)
    @Schema(description = "Short subject line", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subject;

    @NotBlank
    @Size(max = 8000)
    @Schema(description = "Message body", requiredMode = Schema.RequiredMode.REQUIRED)
    private String body;
}
