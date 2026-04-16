package com.ziyara.backend.infrastructure.persistence.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

/**
 * Parse JSONB text/array columns storing UUID strings into domain lists.
 */
public final class UuidListJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private UuidListJson() {}

    public static List<UUID> parse(String json) {
        if (json == null || json.isBlank() || "null".equalsIgnoreCase(json.trim())) {
            return null;
        }
        try {
            List<String> raw = MAPPER.readValue(json, new TypeReference<List<String>>() {});
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            return raw.stream().map(UUID::fromString).toList();
        } catch (Exception e) {
            return null;
        }
    }
}
