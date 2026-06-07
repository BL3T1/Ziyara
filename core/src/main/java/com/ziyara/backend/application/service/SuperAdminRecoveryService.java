package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.UserResponse;
import com.ziyara.backend.application.dto.response.DeletedItemResponse;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Super-admin only: search customers, list soft-deleted users/services, restore by clearing {@code deleted_at}.
 */
@Service
@RequiredArgsConstructor
public class SuperAdminRecoveryService {

    private static final String USERS = "sys_users";
    private static final String CUSTOMERS = "customers";
    private static final String SERVICES = "hotel_services";
    private static final String PROVIDERS = "hotel_service_providers";

    private static final Field<UUID> U_ID = DSL.field(DSL.name(USERS, "id"), UUID.class);
    private static final Field<String> U_EMAIL = DSL.field(DSL.name(USERS, "email"), String.class);
    private static final Field<String> U_PHONE = DSL.field(DSL.name(USERS, "phone"), String.class);
    private static final Field<String> U_ROLE = DSL.field(DSL.name(USERS, "role"), String.class);
    private static final Field<String> U_STATUS = DSL.field(DSL.name(USERS, "status"), String.class);
    private static final Field<Boolean> U_EMAIL_VERIFIED = DSL.field(DSL.name(USERS, "email_verified"), Boolean.class);
    private static final Field<Boolean> U_PHONE_VERIFIED = DSL.field(DSL.name(USERS, "phone_verified"), Boolean.class);
    private static final Field<LocalDateTime> U_LAST_LOGIN = DSL.field(DSL.name(USERS, "last_login_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> U_CREATED = DSL.field(DSL.name(USERS, "created_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> U_DELETED = DSL.field(DSL.name(USERS, "deleted_at"), LocalDateTime.class);

    private static final Field<UUID> C_USER_ID = DSL.field(DSL.name(CUSTOMERS, "user_id"), UUID.class);
    private static final Field<String> C_FIRST_NAME = DSL.field(DSL.name(CUSTOMERS, "first_name"), String.class);
    private static final Field<String> C_LAST_NAME = DSL.field(DSL.name(CUSTOMERS, "last_name"), String.class);

    private static final Field<UUID> S_ID = DSL.field(DSL.name(SERVICES, "id"), UUID.class);
    private static final Field<UUID> S_PROVIDER = DSL.field(DSL.name(SERVICES, "provider_id"), UUID.class);
    private static final Field<String> S_TYPE = DSL.field(DSL.name(SERVICES, "type"), String.class);
    private static final Field<String> S_NAME = DSL.field(DSL.name(SERVICES, "name"), String.class);
    private static final Field<String> S_STATUS = DSL.field(DSL.name(SERVICES, "status"), String.class);
    private static final Field<LocalDateTime> S_DELETED = DSL.field(DSL.name(SERVICES, "deleted_at"), LocalDateTime.class);

    private static final Field<UUID> P_ID = DSL.field(DSL.name(PROVIDERS, "id"), UUID.class);
    private static final Field<String> P_NAME = DSL.field(DSL.name(PROVIDERS, "company_name"), String.class);
    private static final Field<String> P_TYPE = DSL.field(DSL.name(PROVIDERS, "provider_type"), String.class);
    private static final Field<String> P_EMAIL = DSL.field(DSL.name(PROVIDERS, "contact_email"), String.class);
    private static final Field<LocalDateTime> P_DELETED = DSL.field(DSL.name(PROVIDERS, "deleted_at"), LocalDateTime.class);

    private final DSLContext dsl;
    private final UserRepository userRepository;

    public List<UserResponse> searchCustomers(String q, int limit) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        int lim = Math.min(Math.max(limit, 1), 100);
        String term = q.trim();
        Condition base = DSL.field(DSL.name(USERS, "role")).eq(UserRole.CUSTOMER.name())
                .and(DSL.field(DSL.name(USERS, "deleted_at")).isNull());
        Condition match = buildCustomerSearchMatch(term);
        var rows = dsl.select(
                        U_ID,
                        U_EMAIL,
                        U_PHONE,
                        U_ROLE,
                        U_STATUS,
                        U_EMAIL_VERIFIED,
                        U_PHONE_VERIFIED,
                        U_LAST_LOGIN,
                        U_CREATED,
                        C_FIRST_NAME,
                        C_LAST_NAME)
                .from(DSL.table(DSL.name(USERS)))
                .leftJoin(DSL.table(DSL.name(CUSTOMERS))).on(C_USER_ID.eq(U_ID))
                .where(base.and(match))
                .orderBy(U_CREATED.desc())
                .limit(lim)
                .fetch();
        List<UserResponse> out = new ArrayList<>();
        rows.forEach(r -> out.add(toCustomerSearchResponse(r)));
        return out;
    }

    public List<DeletedItemResponse> searchDeleted(String q, int limit, Set<String> allowedTypes) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        int lim = Math.min(Math.max(limit, 1), 100);
        String term = q.trim();
        Condition userMatch = U_DELETED.isNotNull().and(buildDeletedUserTextMatch(term));
        Condition svcMatch = S_DELETED.isNotNull().and(buildDeletedServiceTextMatch(term));

        int half = Math.max(1, lim / 2);
        List<DeletedItemResponse> out = new ArrayList<>();

        dsl.select(U_ID, U_EMAIL, U_ROLE, U_DELETED)
                .from(DSL.table(DSL.name(USERS)))
                .where(userMatch)
                .orderBy(U_DELETED.desc())
                .limit(half)
                .fetch()
                .forEach(r -> out.add(DeletedItemResponse.builder()
                        .entityType("USER")
                        .id(r.get(U_ID))
                        .label(r.get(U_EMAIL))
                        .detail("role=" + r.get(U_ROLE))
                        .deletedAt(r.get(U_DELETED))
                        .build()));

        dsl.select(S_ID, S_NAME, S_TYPE, S_PROVIDER, S_DELETED)
                .from(DSL.table(DSL.name(SERVICES)))
                .where(svcMatch)
                .orderBy(S_DELETED.desc())
                .limit(half)
                .fetch()
                .forEach(r -> out.add(DeletedItemResponse.builder()
                        .entityType("SERVICE")
                        .id(r.get(S_ID))
                        .label(r.get(S_NAME))
                        .detail("type=" + r.get(S_TYPE) + ", provider=" + r.get(S_PROVIDER))
                        .deletedAt(r.get(S_DELETED))
                        .build()));

        Condition providerMatch = P_DELETED.isNotNull().and(
                DSL.lower(P_NAME).contains(DSL.lower(DSL.inline(term)))
                        .or(DSL.lower(P_EMAIL).contains(DSL.lower(DSL.inline(term)))));
        dsl.select(P_ID, P_NAME, P_TYPE, P_EMAIL, P_DELETED)
                .from(DSL.table(DSL.name(PROVIDERS)))
                .where(providerMatch)
                .orderBy(P_DELETED.desc())
                .limit(half)
                .fetch()
                .forEach(r -> out.add(DeletedItemResponse.builder()
                        .entityType("PROVIDER")
                        .id(r.get(P_ID))
                        .label(r.get(P_NAME))
                        .detail("type=" + r.get(P_TYPE) + ", email=" + r.get(P_EMAIL))
                        .deletedAt(r.get(P_DELETED))
                        .build()));

        return out.stream()
                .filter(r -> isEntityAllowed(r, allowedTypes))
                .limit(lim)
                .toList();
    }

    private static boolean isEntityAllowed(DeletedItemResponse r, Set<String> allowedTypes) {
        if (allowedTypes.isEmpty()) return false;
        if (allowedTypes.contains("SERVICE") && "SERVICE".equals(r.getEntityType())) return true;
        if (allowedTypes.contains("PROVIDER") && "PROVIDER".equals(r.getEntityType())) return true;
        if ("USER".equals(r.getEntityType())) {
            boolean isCustomer = r.getDetail() != null && r.getDetail().contains("role=CUSTOMER");
            if (isCustomer) return allowedTypes.contains("USER_CUSTOMER");
            return allowedTypes.contains("USER_COMPANY");
        }
        return false;
    }

    /**
     * Latest soft-deleted users and services (by {@code deleted_at}), merged and sorted newest first.
     */
    public List<DeletedItemResponse> listRecentDeleted(int limit, Set<String> allowedTypes) {
        int lim = Math.min(Math.max(limit, 1), 100);
        int third = Math.max(1, lim / 3);
        List<DeletedItemResponse> out = new ArrayList<>();

        dsl.select(U_ID, U_EMAIL, U_ROLE, U_DELETED)
                .from(DSL.table(DSL.name(USERS)))
                .where(U_DELETED.isNotNull())
                .orderBy(U_DELETED.desc())
                .limit(third)
                .fetch()
                .forEach(r -> out.add(DeletedItemResponse.builder()
                        .entityType("USER")
                        .id(r.get(U_ID))
                        .label(r.get(U_EMAIL))
                        .detail("role=" + r.get(U_ROLE))
                        .deletedAt(r.get(U_DELETED))
                        .build()));

        dsl.select(S_ID, S_NAME, S_TYPE, S_PROVIDER, S_DELETED)
                .from(DSL.table(DSL.name(SERVICES)))
                .where(S_DELETED.isNotNull())
                .orderBy(S_DELETED.desc())
                .limit(third)
                .fetch()
                .forEach(r -> out.add(DeletedItemResponse.builder()
                        .entityType("SERVICE")
                        .id(r.get(S_ID))
                        .label(r.get(S_NAME))
                        .detail("type=" + r.get(S_TYPE) + ", provider=" + r.get(S_PROVIDER))
                        .deletedAt(r.get(S_DELETED))
                        .build()));

        dsl.select(P_ID, P_NAME, P_TYPE, P_EMAIL, P_DELETED)
                .from(DSL.table(DSL.name(PROVIDERS)))
                .where(P_DELETED.isNotNull())
                .orderBy(P_DELETED.desc())
                .limit(third)
                .fetch()
                .forEach(r -> out.add(DeletedItemResponse.builder()
                        .entityType("PROVIDER")
                        .id(r.get(P_ID))
                        .label(r.get(P_NAME))
                        .detail("type=" + r.get(P_TYPE) + ", email=" + r.get(P_EMAIL))
                        .deletedAt(r.get(P_DELETED))
                        .build()));

        out.sort(Comparator.comparing(DeletedItemResponse::getDeletedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return out.stream()
                .filter(r -> isEntityAllowed(r, allowedTypes))
                .limit(lim)
                .toList();
    }

    /**
     * Ensures the user exists, is role CUSTOMER, and is not soft-deleted.
     */
    public void assertActiveCustomer(UUID userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (u.getRole() != UserRole.CUSTOMER || u.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Customer not found");
        }
    }

    @Transactional
    public void restore(String entityType, UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        String t = entityType == null ? "" : entityType.trim().toUpperCase(Locale.ROOT);
        int updated;
        switch (t) {
            case "USER" -> updated = dsl.update(DSL.table(DSL.name(USERS)))
                    .set(U_DELETED, (LocalDateTime) null)
                    .where(U_ID.eq(id).and(U_DELETED.isNotNull()))
                    .execute();
            case "SERVICE" -> updated = dsl.update(DSL.table(DSL.name(SERVICES)))
                    .set(S_DELETED, (LocalDateTime) null)
                    .where(S_ID.eq(id).and(S_DELETED.isNotNull()))
                    .execute();
            case "PROVIDER" -> updated = dsl.update(DSL.table(DSL.name(PROVIDERS)))
                    .set(P_DELETED, (LocalDateTime) null)
                    .where(P_ID.eq(id).and(P_DELETED.isNotNull()))
                    .execute();
            default -> throw new IllegalArgumentException("entityType must be USER, SERVICE, or PROVIDER");
        }
        if (updated == 0) {
            throw new ResourceNotFoundException("No deleted " + t + " found with id " + id);
        }
    }

    /**
     * Hard-deletes a row that has already been soft-deleted. Only operates on rows with deleted_at IS NOT NULL
     * as a safety guard — non-deleted rows cannot be permanently destroyed through this path.
     *
     * For USER: nullifies nullable FKs, deletes dependent rows in FK order, then removes the user row.
     */
    @Transactional
    public void permanentDelete(String entityType, UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        String t = entityType == null ? "" : entityType.trim().toUpperCase(Locale.ROOT);
        int deleted;
        switch (t) {
            case "USER" -> {
                boolean exists = dsl.fetchExists(
                        dsl.selectOne()
                                .from(DSL.table(DSL.name(USERS)))
                                .where(U_ID.eq(id).and(U_DELETED.isNotNull())));
                if (!exists) {
                    throw new ResourceNotFoundException("No soft-deleted USER found with id " + id);
                }
                // 1. Null out nullable FKs that reference this user (no ON DELETE action)
                for (String col : new String[]{"created_by", "approved_by"}) {
                    dsl.update(DSL.table(DSL.name("hotel_service_providers")))
                            .set(DSL.field(DSL.name(col), UUID.class), (UUID) null)
                            .where(DSL.field(DSL.name(col), UUID.class).eq(id))
                            .execute();
                    dsl.update(DSL.table(DSL.name("disc_discount_codes")))
                            .set(DSL.field(DSL.name(col), UUID.class), (UUID) null)
                            .where(DSL.field(DSL.name(col), UUID.class).eq(id))
                            .execute();
                }
                dsl.update(DSL.table(DSL.name("sys_feature_flags")))
                        .set(DSL.field(DSL.name("updated_by"), UUID.class), (UUID) null)
                        .where(DSL.field(DSL.name("updated_by"), UUID.class).eq(id))
                        .execute();
                dsl.update(DSL.table(DSL.name("sys_consent_audit_log")))
                        .set(DSL.field(DSL.name("changed_by"), UUID.class), (UUID) null)
                        .where(DSL.field(DSL.name("changed_by"), UUID.class).eq(id))
                        .execute();
                dsl.update(DSL.table(DSL.name("portal_payout_requests")))
                        .set(DSL.field(DSL.name("processed_by"), UUID.class), (UUID) null)
                        .where(DSL.field(DSL.name("processed_by"), UUID.class).eq(id))
                        .execute();
                // Null booking FK on complaints before deleting bookings (nullable FK, no cascade)
                dsl.update(DSL.table(DSL.name("support_complaints")))
                        .set(DSL.field(DSL.name("booking_id"), UUID.class), (UUID) null)
                        .where(DSL.field(DSL.name("booking_id"), UUID.class).in(
                                dsl.select(DSL.field(DSL.name("id"), UUID.class))
                                        .from(DSL.table(DSL.name("bkg_bookings")))
                                        .where(DSL.field(DSL.name("customer_id"), UUID.class).eq(id))
                        ))
                        .execute();
                // 2. Delete NOT-NULL FK dependents (leaf → root)
                // pay_refunds → pay_payments → bkg_bookings
                dsl.deleteFrom(DSL.table(DSL.name("pay_refunds")))
                        .where(DSL.field(DSL.name("payment_id"), UUID.class).in(
                                dsl.select(DSL.field(DSL.name("id"), UUID.class))
                                        .from(DSL.table(DSL.name("pay_payments")))
                                        .where(DSL.field(DSL.name("booking_id"), UUID.class).in(
                                                dsl.select(DSL.field(DSL.name("id"), UUID.class))
                                                        .from(DSL.table(DSL.name("bkg_bookings")))
                                                        .where(DSL.field(DSL.name("customer_id"), UUID.class).eq(id))
                                        ))
                        ))
                        .execute();
                dsl.deleteFrom(DSL.table(DSL.name("pay_payments")))
                        .where(DSL.field(DSL.name("booking_id"), UUID.class).in(
                                dsl.select(DSL.field(DSL.name("id"), UUID.class))
                                        .from(DSL.table(DSL.name("bkg_bookings")))
                                        .where(DSL.field(DSL.name("customer_id"), UUID.class).eq(id))
                        ))
                        .execute();
                dsl.deleteFrom(DSL.table(DSL.name("bkg_bookings")))
                        .where(DSL.field(DSL.name("customer_id"), UUID.class).eq(id))
                        .execute();
                dsl.deleteFrom(DSL.table(DSL.name("hotel_reviews")))
                        .where(DSL.field(DSL.name("customer_id"), UUID.class).eq(id))
                        .execute();
                // Comments by this user on any complaint must be removed before own complaints cascade
                dsl.deleteFrom(DSL.table(DSL.name("support_complaint_comments")))
                        .where(DSL.field(DSL.name("user_id"), UUID.class).eq(id))
                        .execute();
                // Own complaints (child comments cascade via ON DELETE CASCADE)
                dsl.deleteFrom(DSL.table(DSL.name("support_complaints")))
                        .where(DSL.field(DSL.name("customer_id"), UUID.class).eq(id))
                        .execute();
                // Comments by this user on any ticket before own tickets cascade
                dsl.deleteFrom(DSL.table(DSL.name("support_ticket_comments")))
                        .where(DSL.field(DSL.name("user_id"), UUID.class).eq(id))
                        .execute();
                // Own tickets (child comments cascade via ON DELETE CASCADE)
                dsl.deleteFrom(DSL.table(DSL.name("support_internal_tickets")))
                        .where(DSL.field(DSL.name("reporter_id"), UUID.class).eq(id))
                        .execute();
                // Employee record has ON DELETE RESTRICT (V14) — must be removed before user
                dsl.deleteFrom(DSL.table(DSL.name("sys_employees")))
                        .where(DSL.field(DSL.name("user_id"), UUID.class).eq(id))
                        .execute();
                // 3. Hard-delete the user row
                deleted = dsl.deleteFrom(DSL.table(DSL.name(USERS)))
                        .where(U_ID.eq(id))
                        .execute();
            }
            case "SERVICE" -> {
                // bkg_bookings.service_id has no ON DELETE CASCADE — must clear dependents first.
                // 1. Nullify nullable booking FK on complaints
                dsl.update(DSL.table(DSL.name("support_complaints")))
                        .set(DSL.field(DSL.name("booking_id"), UUID.class), (UUID) null)
                        .where(DSL.field(DSL.name("booking_id"), UUID.class).in(
                                dsl.select(DSL.field(DSL.name("id"), UUID.class))
                                        .from(DSL.table(DSL.name("bkg_bookings")))
                                        .where(DSL.field(DSL.name("service_id"), UUID.class).eq(id))))
                        .execute();
                // 2. pay_refunds → pay_payments → bkg_bookings
                dsl.deleteFrom(DSL.table(DSL.name("pay_refunds")))
                        .where(DSL.field(DSL.name("payment_id"), UUID.class).in(
                                dsl.select(DSL.field(DSL.name("id"), UUID.class))
                                        .from(DSL.table(DSL.name("pay_payments")))
                                        .where(DSL.field(DSL.name("booking_id"), UUID.class).in(
                                                dsl.select(DSL.field(DSL.name("id"), UUID.class))
                                                        .from(DSL.table(DSL.name("bkg_bookings")))
                                                        .where(DSL.field(DSL.name("service_id"), UUID.class).eq(id))))))
                        .execute();
                dsl.deleteFrom(DSL.table(DSL.name("pay_payments")))
                        .where(DSL.field(DSL.name("booking_id"), UUID.class).in(
                                dsl.select(DSL.field(DSL.name("id"), UUID.class))
                                        .from(DSL.table(DSL.name("bkg_bookings")))
                                        .where(DSL.field(DSL.name("service_id"), UUID.class).eq(id))))
                        .execute();
                dsl.deleteFrom(DSL.table(DSL.name("bkg_bookings")))
                        .where(DSL.field(DSL.name("service_id"), UUID.class).eq(id))
                        .execute();
                // 3. hotel_reviews.service_id has no CASCADE
                dsl.deleteFrom(DSL.table(DSL.name("hotel_reviews")))
                        .where(DSL.field(DSL.name("service_id"), UUID.class).eq(id))
                        .execute();
                deleted = dsl.deleteFrom(DSL.table(DSL.name(SERVICES)))
                        .where(S_ID.eq(id).and(S_DELETED.isNotNull()))
                        .execute();
            }
            case "PROVIDER" -> {
                // Subquery for all service IDs under this provider (reused in booking cleanup)
                var svcIds = dsl.select(DSL.field(DSL.name("id"), UUID.class))
                        .from(DSL.table(DSL.name(SERVICES)))
                        .where(S_PROVIDER.eq(id));
                // 1. Nullify nullable booking FK on complaints
                dsl.update(DSL.table(DSL.name("support_complaints")))
                        .set(DSL.field(DSL.name("booking_id"), UUID.class), (UUID) null)
                        .where(DSL.field(DSL.name("booking_id"), UUID.class).in(
                                dsl.select(DSL.field(DSL.name("id"), UUID.class))
                                        .from(DSL.table(DSL.name("bkg_bookings")))
                                        .where(DSL.field(DSL.name("service_id"), UUID.class).in(svcIds))))
                        .execute();
                // 2. pay_refunds → pay_payments → bkg_bookings
                dsl.deleteFrom(DSL.table(DSL.name("pay_refunds")))
                        .where(DSL.field(DSL.name("payment_id"), UUID.class).in(
                                dsl.select(DSL.field(DSL.name("id"), UUID.class))
                                        .from(DSL.table(DSL.name("pay_payments")))
                                        .where(DSL.field(DSL.name("booking_id"), UUID.class).in(
                                                dsl.select(DSL.field(DSL.name("id"), UUID.class))
                                                        .from(DSL.table(DSL.name("bkg_bookings")))
                                                        .where(DSL.field(DSL.name("service_id"), UUID.class).in(svcIds))))))
                        .execute();
                dsl.deleteFrom(DSL.table(DSL.name("pay_payments")))
                        .where(DSL.field(DSL.name("booking_id"), UUID.class).in(
                                dsl.select(DSL.field(DSL.name("id"), UUID.class))
                                        .from(DSL.table(DSL.name("bkg_bookings")))
                                        .where(DSL.field(DSL.name("service_id"), UUID.class).in(svcIds))))
                        .execute();
                dsl.deleteFrom(DSL.table(DSL.name("bkg_bookings")))
                        .where(DSL.field(DSL.name("service_id"), UUID.class).in(svcIds))
                        .execute();
                // 3. hotel_reviews.service_id has no CASCADE
                dsl.deleteFrom(DSL.table(DSL.name("hotel_reviews")))
                        .where(DSL.field(DSL.name("service_id"), UUID.class).in(svcIds))
                        .execute();
                // 4. hotel_services (images/rooms/menu cascade via ON DELETE CASCADE)
                dsl.deleteFrom(DSL.table(DSL.name(SERVICES)))
                        .where(S_PROVIDER.eq(id))
                        .execute();
                // 5. provider_subscriptions (no FK constraint, but clean up the orphan)
                dsl.deleteFrom(DSL.table(DSL.name("provider_subscriptions")))
                        .where(DSL.field(DSL.name("provider_id"), UUID.class).eq(id))
                        .execute();
                deleted = dsl.deleteFrom(DSL.table(DSL.name(PROVIDERS)))
                        .where(P_ID.eq(id).and(P_DELETED.isNotNull()))
                        .execute();
            }
            default -> throw new IllegalArgumentException("entityType must be USER, SERVICE, or PROVIDER");
        }
        if (deleted == 0) {
            throw new ResourceNotFoundException("No soft-deleted " + t + " found with id " + id);
        }
    }

    /** Deleted recycle search: email / phone only (no UUID lookup). */
    private static Condition buildDeletedUserTextMatch(String term) {
        String pattern = "%" + term.replace("%", "\\%").replace("_", "\\_") + "%";
        return U_EMAIL.likeIgnoreCase(pattern).or(U_PHONE.contains(term));
    }

    /** Deleted recycle search: service name only (no service or provider UUID lookup). */
    private static Condition buildDeletedServiceTextMatch(String term) {
        String pattern = "%" + term.replace("%", "\\%").replace("_", "\\_") + "%";
        return S_NAME.likeIgnoreCase(pattern);
    }

    /**
     * Customer directory search: email, phone, exact id, and profile name (first, last, or full via {@code concat_ws}).
     */
    private static Condition buildCustomerSearchMatch(String term) {
        try {
            UUID uuid = UUID.fromString(term);
            return U_ID.eq(uuid);
        } catch (IllegalArgumentException e) {
            String pattern = "%" + term.replace("%", "\\%").replace("_", "\\_") + "%";
            Field<String> fullName = DSL.field(
                    "concat_ws(' ', {0}, {1})",
                    String.class,
                    C_FIRST_NAME,
                    C_LAST_NAME);
            return U_EMAIL.likeIgnoreCase(pattern)
                    .or(U_PHONE.contains(term))
                    .or(C_FIRST_NAME.likeIgnoreCase(pattern))
                    .or(C_LAST_NAME.likeIgnoreCase(pattern))
                    .or(fullName.likeIgnoreCase(pattern));
        }
    }

    private static UserResponse toCustomerSearchResponse(org.jooq.Record r) {
        String fn = r.get(C_FIRST_NAME);
        String ln = r.get(C_LAST_NAME);
        String fullName = buildDisplayFullName(fn, ln);
        return UserResponse.builder()
                .id(r.get(U_ID))
                .email(r.get(U_EMAIL))
                .phone(r.get(U_PHONE))
                .role(parseRole(r.get(U_ROLE)))
                .status(parseStatus(r.get(U_STATUS)))
                .emailVerified(r.get(U_EMAIL_VERIFIED) != null && Boolean.TRUE.equals(r.get(U_EMAIL_VERIFIED)))
                .phoneVerified(r.get(U_PHONE_VERIFIED) != null && Boolean.TRUE.equals(r.get(U_PHONE_VERIFIED)))
                .lastLoginAt(r.get(U_LAST_LOGIN))
                .createdAt(r.get(U_CREATED))
                .firstName(fn)
                .lastName(ln)
                .fullName(fullName)
                .build();
    }

    private static String buildDisplayFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return null;
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    private static com.ziyara.backend.domain.enums.UserRole parseRole(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return com.ziyara.backend.domain.enums.UserRole.valueOf(v.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static com.ziyara.backend.domain.enums.UserStatus parseStatus(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return com.ziyara.backend.domain.enums.UserStatus.valueOf(v.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
