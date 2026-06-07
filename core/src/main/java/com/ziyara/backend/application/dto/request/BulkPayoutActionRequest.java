package com.ziyara.backend.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkPayoutActionRequest {

    @NotEmpty
    private List<UUID> ids;

    private String notes;
}
