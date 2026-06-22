package com.ziyara.backend.infrastructure.persistence.converter;

import com.ziyara.backend.domain.enums.UserRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts UserRole to/from its DB string representation.
 * Any unrecognised DB value (e.g. old enum names written before V55) maps
 * to STAFF so the row remains loadable during the migration window.
 */
@Converter
public class UserRoleAttributeConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(UserRole attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return UserRole.STAFF;
        return switch (dbData.trim()) {
            case "SUPER_ADMIN" -> UserRole.SUPER_ADMIN;
            case "CUSTOMER"    -> UserRole.CUSTOMER;
            default            -> UserRole.STAFF;
        };
    }
}
