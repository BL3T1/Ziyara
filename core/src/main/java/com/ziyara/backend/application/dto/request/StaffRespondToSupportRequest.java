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
@Schema(description = "Staff response to a provider support request")
public class StaffRespondToSupportRequest {

    @NotBlank
    @Size(max = 8000)
    @Schema(description = "Response text from staff", requiredMode = Schema.RequiredMode.REQUIRED)
    private String response;
}
