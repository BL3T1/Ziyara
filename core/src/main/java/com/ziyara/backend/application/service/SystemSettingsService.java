package com.ziyara.backend.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.ziyara.backend.application.annotation.Audited;
import com.ziyara.backend.application.dto.request.UpdateSystemSettingsRequest;
import com.ziyara.backend.application.dto.response.SystemSettingsResponse;
import com.ziyara.backend.domain.entity.SystemSetting;
import com.ziyara.backend.domain.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemSettingsService {

    public static final String KEY_COMPANY_DISPLAY_NAME = "company.display_name";
    public static final String KEY_DEFAULT_CURRENCY = "platform.default_currency";
    public static final String KEY_MAINTENANCE_MODE = "platform.maintenance_mode";
    public static final String KEY_PROVIDER_MAINTENANCE_MODE = "platform.provider_maintenance_mode";

    private static final String DEFAULT_COMPANY = "Ziyara";
    private static final String DEFAULT_CURRENCY = "USD";

    private final SystemSettingRepository repository;
    private final ObjectMapper objectMapper;

    @Cacheable("systemSettings")
    @Transactional(readOnly = true)
    public SystemSettingsResponse getSettings() {
        return SystemSettingsResponse.builder()
                .companyDisplayName(readString(KEY_COMPANY_DISPLAY_NAME, DEFAULT_COMPANY))
                .defaultCurrency(readString(KEY_DEFAULT_CURRENCY, DEFAULT_CURRENCY).toUpperCase())
                .maintenanceMode(readBoolean(KEY_MAINTENANCE_MODE))
                .providerMaintenanceMode(readBoolean(KEY_PROVIDER_MAINTENANCE_MODE))
                .build();
    }

    @Audited(action = "SETTINGS_UPDATE", entityType = "SystemSettings")
    @CacheEvict(value = "systemSettings", allEntries = true)
    @Transactional
    public SystemSettingsResponse update(UpdateSystemSettingsRequest request, UUID updatedBy) {
        Instant now = Instant.now();
        if (request.getCompanyDisplayName() != null) {
            upsert(KEY_COMPANY_DISPLAY_NAME, TextNode.valueOf(request.getCompanyDisplayName().trim()), updatedBy, now);
        }
        if (request.getDefaultCurrency() != null) {
            upsert(KEY_DEFAULT_CURRENCY, TextNode.valueOf(request.getDefaultCurrency().trim().toUpperCase()), updatedBy, now);
        }
        if (request.getMaintenanceMode() != null) {
            upsert(KEY_MAINTENANCE_MODE, BooleanNode.valueOf(request.getMaintenanceMode()), updatedBy, now);
        }
        if (request.getProviderMaintenanceMode() != null) {
            upsert(KEY_PROVIDER_MAINTENANCE_MODE, BooleanNode.valueOf(request.getProviderMaintenanceMode()), updatedBy, now);
        }
        return getSettings();
    }

    private void upsert(String key, JsonNode value, UUID updatedBy, Instant now) {
        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.set("v", value);
        String json = wrapper.toString();
        SystemSetting setting = repository.findByKey(key)
                .orElseGet(() -> {
                    SystemSetting s = new SystemSetting();
                    s.setSettingKey(key);
                    return s;
                });
        setting.setValueJson(json);
        setting.setUpdatedAt(now);
        setting.setUpdatedBy(updatedBy);
        repository.save(setting);
    }

    private String readString(String key, String defaultValue) {
        return repository.findByKey(key)
                .map(SystemSetting::getValueJson)
                .map(json -> parseVAsString(json, defaultValue))
                .orElse(defaultValue);
    }

    private boolean readBoolean(String key) {
        return repository.findByKey(key)
                .map(SystemSetting::getValueJson)
                .map(this::parseVAsBoolean)
                .orElse(false);
    }

    private String parseVAsString(String json, String defaultValue) {
        try {
            JsonNode n = objectMapper.readTree(json).get("v");
            if (n == null || !n.isTextual()) {
                return defaultValue;
            }
            String s = n.asText();
            return s == null || s.isBlank() ? defaultValue : s;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean parseVAsBoolean(String json) {
        try {
            JsonNode n = objectMapper.readTree(json).get("v");
            return n != null && n.isBoolean() && n.booleanValue();
        } catch (Exception e) {
            return false;
        }
    }
}
