package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.UserConsent;

import java.util.List;
import java.util.UUID;

public interface UserConsentRepository {

    UserConsent save(UserConsent consent);

    List<UserConsent> findByUserIdOrderedDesc(UUID userId);
}
