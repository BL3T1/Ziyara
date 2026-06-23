package com.ziyara.backend.domain.common;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Typed wrapper over a free-form key-value map (amenities, service attributes, etc.).
 * Replaces raw {@code Map<String, Object>} in domain entities so callers get typed accessors
 * and the shape is explicit rather than a cast-everywhere bag.
 */
public final class Attributes {

    private final Map<String, Object> values;

    private Attributes(Map<String, Object> values) {
        this.values = values == null ? Collections.emptyMap() : Collections.unmodifiableMap(values);
    }

    public static Attributes of(Map<String, Object> values) {
        return new Attributes(values);
    }

    public static Attributes empty() {
        return new Attributes(null);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public Optional<String> getString(String key) {
        Object v = values.get(key);
        return v instanceof String s ? Optional.of(s) : Optional.empty();
    }

    public Optional<Integer> getInt(String key) {
        Object v = values.get(key);
        if (v instanceof Integer i) return Optional.of(i);
        if (v instanceof Number n) return Optional.of(n.intValue());
        return Optional.empty();
    }

    public Optional<Boolean> getBoolean(String key) {
        Object v = values.get(key);
        return v instanceof Boolean b ? Optional.of(b) : Optional.empty();
    }

    /** Returns the raw backing map for persistence adapters. Never mutate the returned map. */
    public Map<String, Object> toMap() {
        return values;
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
