package com.ziyara.backend.modules.provider.api;

import com.ziyara.backend.domain.entity.ProviderProfileEditRequest;

import java.util.Map;
import java.util.UUID;

public interface ProviderProfileEditApi {
    ProviderProfileEditRequest submitEditRequest(UUID providerId, UUID requestedBy, Map<String, Object> newValues);
    ProviderProfileEditRequest getLatestForProvider(UUID providerId);
}
