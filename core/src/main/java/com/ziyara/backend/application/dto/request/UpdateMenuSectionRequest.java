package com.ziyara.backend.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMenuSectionRequest {

    @Size(max = 255)
    private String title;

    private Integer sortOrder;
}
