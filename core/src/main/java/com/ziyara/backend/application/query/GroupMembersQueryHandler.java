package com.ziyara.backend.application.query;

import com.ziyara.backend.application.dto.UserResponse;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lists users belonging to an organizational group (primary {@code sys_users.role} or {@code sys_user_roles}).
 */
@Component
@RequiredArgsConstructor
public class GroupMembersQueryHandler {

    public static final UUID UNGROUPED_GROUP_ID = UUID.fromString("00000000-0000-4000-8000-0000000000ff");

    private static final String USERS = "sys_users";
    private static final String USER_ROLES = "sys_user_roles";

    private static final Field<UUID> F_ID = DSL.field(DSL.name(USERS, "id"), UUID.class);
    private static final Field<String> F_EMAIL    = DSL.field(DSL.name(USERS, "email"),    String.class);
    private static final Field<String> F_USERNAME = DSL.field(DSL.name(USERS, "username"), String.class);
    private static final Field<String> F_PHONE    = DSL.field(DSL.name(USERS, "phone"),    String.class);
    private static final Field<String> F_ROLE = DSL.field(DSL.name(USERS, "role"), String.class);
    private static final Field<String> F_STATUS = DSL.field(DSL.name(USERS, "status"), String.class);
    private static final Field<Boolean> F_EMAIL_VERIFIED = DSL.field(DSL.name(USERS, "email_verified"), Boolean.class);
    private static final Field<Boolean> F_PHONE_VERIFIED = DSL.field(DSL.name(USERS, "phone_verified"), Boolean.class);
    private static final Field<LocalDateTime> F_LAST_LOGIN_AT = DSL.field(DSL.name(USERS, "last_login_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> F_CREATED_AT = DSL.field(DSL.name(USERS, "created_at"), LocalDateTime.class);

    private final DSLContext dsl;
    private final RoleRepository roleRepository;

    public Page<UserResponse> findUsersInGroup(UUID groupId, int page, int size) {
        int p = Math.max(0, page);
        int s = size <= 0 ? 20 : size;
        int offset = p * s;

        List<Role> inGroup = UNGROUPED_GROUP_ID.equals(groupId)
                ? roleRepository.findByGroupIdIsNullOrderByName()
                : roleRepository.findByGroupIdOrderByName(groupId);

        if (inGroup.isEmpty()) {
            return new PageImpl<>(List.of(), PageRequest.of(p, s), 0);
        }

        List<UUID> roleIds = inGroup.stream().map(Role::getId).toList();
        List<String> codes = inGroup.stream()
                .map(Role::getCode)
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        Field<UUID> urUser = DSL.field(DSL.name(USER_ROLES, "user_id"), UUID.class);
        Field<UUID> urRole = DSL.field(DSL.name(USER_ROLES, "role_id"), UUID.class);
        var assignedSubquery = DSL.select(urUser)
                .from(DSL.table(DSL.name(USER_ROLES)))
                .where(urRole.in(roleIds));

        Condition condition = DSL.field(DSL.name(USERS, "deleted_at")).isNull();
        if (codes.isEmpty()) {
            condition = condition.and(F_ID.in(assignedSubquery));
        } else {
            condition = condition.and(F_ROLE.in(codes).or(F_ID.in(assignedSubquery)));
        }

        var table = DSL.table(DSL.name(USERS));
        long total = dsl.fetchCount(DSL.selectFrom(table).where(condition));

        List<UserResponse> content = dsl.select(F_ID, F_EMAIL, F_PHONE, F_ROLE, F_STATUS, F_EMAIL_VERIFIED, F_PHONE_VERIFIED, F_LAST_LOGIN_AT, F_CREATED_AT)
                .from(table)
                .where(condition)
                .orderBy(F_CREATED_AT.desc())
                .limit(s)
                .offset(offset)
                .fetch()
                .map(this::toUserResponse);

        return new PageImpl<>(content, PageRequest.of(p, s), total);
    }

    private UserResponse toUserResponse(Record r) {
        return UserResponse.builder()
                .id(r.get(F_ID))
                .email(r.get(F_EMAIL))
                .username(safeGet(r, F_USERNAME))
                .phone(r.get(F_PHONE))
                .role(parseRole(r.get(F_ROLE)))
                .status(parseStatus(r.get(F_STATUS)))
                .emailVerified(r.get(F_EMAIL_VERIFIED) != null && Boolean.TRUE.equals(r.get(F_EMAIL_VERIFIED)))
                .phoneVerified(r.get(F_PHONE_VERIFIED) != null && Boolean.TRUE.equals(r.get(F_PHONE_VERIFIED)))
                .lastLoginAt(r.get(F_LAST_LOGIN_AT))
                .createdAt(r.get(F_CREATED_AT))
                .build();
    }

    private static <T> T safeGet(Record r, Field<T> field) {
        try { return r.get(field); } catch (Exception e) { return null; }
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
