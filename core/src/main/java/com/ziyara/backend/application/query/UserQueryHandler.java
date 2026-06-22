package com.ziyara.backend.application.query;

import com.ziyara.backend.application.dto.UserResponse;
import com.ziyara.backend.application.dto.response.LoginHistoryEntryResponse;
import com.ziyara.backend.application.query.dto.UserListQuery;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Query handler for user reads (CQRS query side, jOOQ).
 * Used for GET /users and GET /users/{id} to avoid loading JPA entities.
 * Table name must match {@link com.ziyara.backend.infrastructure.persistence.entity.UserJpaEntity} ({@code sys_users}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserQueryHandler {

    private static final String USERS = "sys_users";
    private static final Field<UUID> F_ID = DSL.field(DSL.name(USERS, "id"), UUID.class);
    private static final Field<String> F_EMAIL = DSL.field(DSL.name(USERS, "email"), String.class);
    private static final Field<String> F_PHONE = DSL.field(DSL.name(USERS, "phone"), String.class);
    private static final Field<String> F_ROLE = DSL.field(DSL.name(USERS, "role"), String.class);
    private static final Field<String> F_STATUS = DSL.field(DSL.name(USERS, "status"), String.class);
    private static final Field<Boolean> F_EMAIL_VERIFIED = DSL.field(DSL.name(USERS, "email_verified"), Boolean.class);
    private static final Field<Boolean> F_PHONE_VERIFIED = DSL.field(DSL.name(USERS, "phone_verified"), Boolean.class);
    private static final Field<LocalDateTime> F_LAST_LOGIN_AT = DSL.field(DSL.name(USERS, "last_login_at"), LocalDateTime.class);
    private static final Field<String> F_LAST_LOGIN_IP = DSL.field(DSL.name(USERS, "last_login_ip"), String.class);
    private static final Field<LocalDateTime> F_CREATED_AT = DSL.field(DSL.name(USERS, "created_at"), LocalDateTime.class);
    private static final Field<Boolean> F_MFA_ENABLED = DSL.field(DSL.name(USERS, "mfa_enabled"), Boolean.class);
    private static final Field<Boolean> F_MARKETING_OPT_IN = DSL.field(DSL.name(USERS, "marketing_opt_in"), Boolean.class);

    private static final Field<String> F_USERNAME   = DSL.field(DSL.name(USERS, "username"),   String.class);
    private static final Field<String> F_FIRST_NAME = DSL.field(DSL.name(USERS, "first_name"), String.class);
    private static final Field<String> F_LAST_NAME  = DSL.field(DSL.name(USERS, "last_name"),  String.class);

    private static final String CUSTOMERS = "customers";
    private static final Field<String> C_FIRST_NAME = DSL.field(DSL.name(CUSTOMERS, "first_name"), String.class);
    private static final Field<String> C_LAST_NAME  = DSL.field(DSL.name(CUSTOMERS, "last_name"),  String.class);

    private final DSLContext dsl;

    /**
     * Returns login history for the user. Uses last_login_at on sys_users (single most recent login).
     * Only selects last_login_at to avoid 500 when last_login_ip column is missing in DB.
     * Returns empty list on any error.
     */
    public List<LoginHistoryEntryResponse> getLoginHistory(UUID userId) {
        try {
            var record = dsl.select(F_LAST_LOGIN_AT)
                    .from(DSL.table(DSL.name(USERS)))
                    .where(F_ID.eq(userId))
                    .fetchOne();
            if (record == null || record.get(F_LAST_LOGIN_AT) == null) {
                return List.of();
            }
            return List.of(LoginHistoryEntryResponse.builder()
                    .loginAt(record.get(F_LAST_LOGIN_AT))
                    .ipAddress(null)
                    .build());
        } catch (Throwable e) {
            log.warn("getLoginHistory failed for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Resolves a user's UUID from their email address using a plain jOOQ lookup.
     * Used by {@code UserController} endpoints that accept email as a path variable,
     * eliminating the need to inject the domain-layer {@code UserRepository} into the
     * presentation layer.
     *
     * @param email normalised (lower-case, trimmed) email address
     * @return the user's UUID
     * @throws com.ziyara.backend.application.exception.ResourceNotFoundException if no user found
     */
    public UUID findUserIdByEmail(String email) {
        UUID id = dsl.select(F_ID)
                .from(DSL.table(DSL.name(USERS)))
                .where(F_EMAIL.equalIgnoreCase(email))
                .fetchOne(F_ID);
        if (id == null) {
            throw new com.ziyara.backend.application.exception.ResourceNotFoundException(
                    "User not found for email: " + email);
        }
        return id;
    }

    @Cacheable(value = "userProfile", key = "#id")
    public UserResponse findByIdCached(UUID id) {
        return findById(id).orElse(null);
    }

    public Optional<UserResponse> findById(UUID id) {
        var record = dsl.select(F_ID, F_EMAIL, F_PHONE, F_ROLE, F_STATUS, F_EMAIL_VERIFIED, F_PHONE_VERIFIED,
                        F_MFA_ENABLED, F_MARKETING_OPT_IN, F_LAST_LOGIN_AT, F_CREATED_AT,
                        F_FIRST_NAME, F_LAST_NAME, C_FIRST_NAME, C_LAST_NAME)
                .from(DSL.table(DSL.name(USERS)))
                .leftJoin(DSL.table(DSL.name(CUSTOMERS)))
                    .on(DSL.field(DSL.name(CUSTOMERS, "user_id"), UUID.class).eq(F_ID))
                .where(F_ID.eq(id))
                .fetchOne();

        return Optional.ofNullable(record).map(this::toUserResponse);
    }

    public org.springframework.data.domain.Page<UserResponse> findPage(UserListQuery query) {
        int page = Math.max(0, query.getPage());
        int size = query.getSize() <= 0 ? 20 : query.getSize();
        int offset = page * size;

        var table = DSL.table(DSL.name(USERS));
        var condition = DSL.noCondition();
        if (query.getStatus() != null) {
            condition = condition.and(DSL.field(DSL.name(USERS, "status")).eq(query.getStatus().name()));
        }
        if (query.getRole() != null) {
            condition = condition.and(DSL.field(DSL.name(USERS, "role")).eq(query.getRole().name()));
        }
        // Company directory: hide customer accounts
        condition = condition.and(DSL.field(DSL.name(USERS, "role")).ne("CUSTOMER"));
        condition = condition.and(DSL.field(DSL.name(USERS, "deleted_at")).isNull());

        long total = dsl.fetchCount(DSL.selectFrom(table).where(condition));

        var orderByCreated = DSL.field(DSL.name(USERS, "created_at")).desc();
        List<UserResponse> content = dsl.select(F_ID, F_EMAIL, F_PHONE, F_ROLE, F_STATUS,
                        F_EMAIL_VERIFIED, F_PHONE_VERIFIED, F_MFA_ENABLED, F_MARKETING_OPT_IN,
                        F_LAST_LOGIN_AT, F_CREATED_AT, F_FIRST_NAME, F_LAST_NAME)
                .from(table)
                .where(condition)
                .orderBy(orderByCreated)
                .limit(size)
                .offset(offset)
                .fetch()
                .map(this::toUserResponse);

        return new org.springframework.data.domain.PageImpl<>(
                content,
                org.springframework.data.domain.PageRequest.of(page, size),
                total
        );
    }

    private UserResponse toUserResponse(Record r) {
        // Prefer customers table (B2C profile); fall back to sys_users columns (staff/admin).
        String firstName = coalesce(safeGet(r, C_FIRST_NAME), safeGet(r, F_FIRST_NAME));
        String lastName  = coalesce(safeGet(r, C_LAST_NAME),  safeGet(r, F_LAST_NAME));
        String fullName  = (firstName != null && lastName != null) ? firstName + " " + lastName
                         : (firstName != null) ? firstName
                         : lastName;
        return UserResponse.builder()
                .id(r.get(F_ID))
                .email(r.get(F_EMAIL))
                .username(safeGet(r, F_USERNAME))
                .phone(r.get(F_PHONE))
                .role(parseRole(r.get(F_ROLE)))
                .status(parseStatus(r.get(F_STATUS)))
                .emailVerified(r.get(F_EMAIL_VERIFIED) != null && Boolean.TRUE.equals(r.get(F_EMAIL_VERIFIED)))
                .phoneVerified(r.get(F_PHONE_VERIFIED) != null && Boolean.TRUE.equals(r.get(F_PHONE_VERIFIED)))
                .mfaEnabled(r.get(F_MFA_ENABLED))
                .marketingOptIn(r.get(F_MARKETING_OPT_IN))
                .lastLoginAt(r.get(F_LAST_LOGIN_AT))
                .createdAt(r.get(F_CREATED_AT))
                .firstName(firstName)
                .lastName(lastName)
                .fullName(fullName)
                .build();
    }

    private static <T> T safeGet(Record r, Field<T> field) {
        try { return r.get(field); } catch (Exception e) { return null; }
    }

    private static String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private static UserRole parseRole(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return UserRole.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static UserStatus parseStatus(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return UserStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
