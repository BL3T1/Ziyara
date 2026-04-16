package com.ziyara.backend.application.query;

import com.ziyara.backend.application.dto.response.DiscountResponse;
import com.ziyara.backend.application.locale.RequestLocaleHolder;
import com.ziyara.backend.domain.enums.DiscountStatus;
import com.ziyara.backend.infrastructure.persistence.json.UuidListJson;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Query handler for discount reads (CQRS query side, jOOQ).
 * GET /discounts (list with status filter), GET /discounts/{id}.
 */
@Component
@RequiredArgsConstructor
public class DiscountQueryHandler {

    private static final String TABLE = "disc_discount_codes";
    private static final Field<UUID> F_ID = DSL.field(DSL.name(TABLE, "id"), UUID.class);
    private static final Field<String> F_CODE = DSL.field(DSL.name(TABLE, "code"), String.class);
    private static final Field<String> F_DESCRIPTION = DSL.field(DSL.name(TABLE, "description"), String.class);
    private static final Field<String> F_DESCRIPTION_AR = DSL.field(DSL.name(TABLE, "description_ar"), String.class);
    private static final Field<String> F_TYPE = DSL.field(DSL.name(TABLE, "type"), String.class);
    private static final Field<BigDecimal> F_VALUE = DSL.field(DSL.name(TABLE, "value"), BigDecimal.class);
    private static final Field<BigDecimal> F_MIN = DSL.field(DSL.name(TABLE, "min_booking_amount"), BigDecimal.class);
    private static final Field<BigDecimal> F_MAX = DSL.field(DSL.name(TABLE, "max_discount_amount"), BigDecimal.class);
    private static final Field<LocalDateTime> F_START = DSL.field(DSL.name(TABLE, "start_date"), LocalDateTime.class);
    private static final Field<LocalDateTime> F_END = DSL.field(DSL.name(TABLE, "end_date"), LocalDateTime.class);
    private static final Field<Integer> F_LIMIT = DSL.field(DSL.name(TABLE, "usage_limit"), Integer.class);
    private static final Field<Integer> F_COUNT = DSL.field(DSL.name(TABLE, "usage_count"), Integer.class);
    private static final Field<String> F_STATUS = DSL.field(DSL.name(TABLE, "status"), String.class);
    private static final Field<LocalDateTime> F_CREATED = DSL.field(DSL.name(TABLE, "created_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> F_UPDATED = DSL.field(DSL.name(TABLE, "updated_at"), LocalDateTime.class);
    private static final Field<String> F_SPONSOR = DSL.field(DSL.name(TABLE, "sponsor"), String.class);
    private static final Field<UUID> F_PROVIDER_ID = DSL.field(DSL.name(TABLE, "provider_id"), UUID.class);
    private static final Field<String> F_APPLICABLE_SVC = DSL.field(DSL.name(TABLE, "applicable_service_ids"), String.class);
    private static final Field<String> F_APPLICABLE_MENU_SEC = DSL.field(DSL.name(TABLE, "applicable_menu_section_ids"), String.class);
    private static final Field<String> F_APPLICABLE_MENU_ITEM = DSL.field(DSL.name(TABLE, "applicable_menu_item_ids"), String.class);
    private static final Field<String> F_APPLICABLE_ROOM = DSL.field(DSL.name(TABLE, "applicable_room_type_ids"), String.class);

    private final DSLContext dsl;

    public Optional<DiscountResponse> findById(UUID id) {
        var record = dsl.select(
                        F_ID, F_CODE, F_DESCRIPTION, F_DESCRIPTION_AR, F_TYPE, F_VALUE, F_MIN, F_MAX, F_START, F_END, F_LIMIT, F_COUNT,
                        F_STATUS, F_CREATED, F_UPDATED, F_SPONSOR, F_PROVIDER_ID, F_APPLICABLE_SVC, F_APPLICABLE_MENU_SEC, F_APPLICABLE_MENU_ITEM, F_APPLICABLE_ROOM)
                .from(DSL.table(DSL.name(TABLE)))
                .where(F_ID.eq(id))
                .fetchOne();
        return Optional.ofNullable(record).map(this::toResponse);
    }

    public org.springframework.data.domain.Page<DiscountResponse> findPage(int page, int size, DiscountStatus status) {
        int offset = page * size;
        Condition condition = DSL.noCondition();
        if (status != null) {
            condition = condition.and(F_STATUS.eq(status.name()));
        }
        var table = DSL.table(DSL.name(TABLE));
        long total = dsl.fetchCount(dsl.selectFrom(table).where(condition));
        List<DiscountResponse> content = dsl.select(
                        F_ID, F_CODE, F_DESCRIPTION, F_DESCRIPTION_AR, F_TYPE, F_VALUE, F_MIN, F_MAX, F_START, F_END, F_LIMIT, F_COUNT,
                        F_STATUS, F_CREATED, F_UPDATED, F_SPONSOR, F_PROVIDER_ID, F_APPLICABLE_SVC, F_APPLICABLE_MENU_SEC, F_APPLICABLE_MENU_ITEM, F_APPLICABLE_ROOM)
                .from(table)
                .where(condition)
                .orderBy(F_CREATED.desc())
                .limit(size)
                .offset(offset)
                .fetch()
                .map(this::toResponse);
        return new org.springframework.data.domain.PageImpl<>(
                content,
                org.springframework.data.domain.PageRequest.of(page, size),
                total
        );
    }

    private DiscountResponse toResponse(Record r) {
        return DiscountResponse.builder()
                .id(r.get(F_ID))
                .code(r.get(F_CODE))
                .description(RequestLocaleHolder.localized(r.get(F_DESCRIPTION), r.get(F_DESCRIPTION_AR)))
                .type(r.get(F_TYPE))
                .value(r.get(F_VALUE))
                .minBookingAmount(r.get(F_MIN))
                .maxDiscountAmount(r.get(F_MAX))
                .startDate(r.get(F_START))
                .endDate(r.get(F_END))
                .usageLimit(r.get(F_LIMIT))
                .usageCount(r.get(F_COUNT) != null ? r.get(F_COUNT) : 0)
                .status(parseStatus(r.get(F_STATUS)))
                .createdAt(r.get(F_CREATED))
                .updatedAt(r.get(F_UPDATED))
                .sponsor(r.get(F_SPONSOR) != null ? r.get(F_SPONSOR) : "COMPANY")
                .providerId(r.get(F_PROVIDER_ID))
                .applicableServiceIds(UuidListJson.parse(r.get(F_APPLICABLE_SVC)))
                .applicableMenuSectionIds(UuidListJson.parse(r.get(F_APPLICABLE_MENU_SEC)))
                .applicableMenuItemIds(UuidListJson.parse(r.get(F_APPLICABLE_MENU_ITEM)))
                .applicableRoomTypeIds(UuidListJson.parse(r.get(F_APPLICABLE_ROOM)))
                .build();
    }

    private static DiscountStatus parseStatus(Object v) {
        if (v == null) return null;
        try {
            return DiscountStatus.valueOf(v.toString().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
