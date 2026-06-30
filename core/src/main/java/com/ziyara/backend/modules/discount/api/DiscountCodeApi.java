package com.ziyara.backend.modules.discount.api;

import com.ziyara.backend.application.dto.request.CreateDiscountRequest;
import com.ziyara.backend.application.dto.response.DiscountResponse;

import java.util.UUID;

public interface DiscountCodeApi {
    DiscountResponse create(CreateDiscountRequest request, UUID createdBy, boolean canApprove);
    DiscountResponse deactivate(UUID id);
}
