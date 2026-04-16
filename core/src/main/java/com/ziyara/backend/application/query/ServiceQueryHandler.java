package com.ziyara.backend.application.query;

import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.application.locale.RequestLocaleHolder;
import com.ziyara.backend.domain.enums.ServiceStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Query handler for service reads (CQRS query side, jOOQ).
 * GET /services (list), GET /services/search, GET /services/{id}.
 */
@Component
@RequiredArgsConstructor
public class ServiceQueryHandler {

    private static final String TABLE = "hotel_services";
    private static final Field<UUID> F_ID = DSL.field(DSL.name(TABLE, "id"), UUID.class);
    private static final Field<UUID> F_PROVIDER_ID = DSL.field(DSL.name(TABLE, "provider_id"), UUID.class);
    private static final Field<String> F_TYPE = DSL.field(DSL.name(TABLE, "type"), String.class);
    private static final Field<String> F_NAME = DSL.field(DSL.name(TABLE, "name"), String.class);
    private static final Field<String> F_DESCRIPTION = DSL.field(DSL.name(TABLE, "description"), String.class);
    private static final Field<String> F_CITY = DSL.field(DSL.name(TABLE, "city"), String.class);
    private static final Field<String> F_COUNTRY = DSL.field(DSL.name(TABLE, "country"), String.class);
    private static final Field<String> F_ADDRESS = DSL.field(DSL.name(TABLE, "address"), String.class);
    private static final Field<BigDecimal> F_BASE_PRICE = DSL.field(DSL.name(TABLE, "base_price"), BigDecimal.class);
    private static final Field<String> F_CURRENCY = DSL.field(DSL.name(TABLE, "currency"), String.class);
    private static final Field<String> F_STATUS = DSL.field(DSL.name(TABLE, "status"), String.class);
    private static final Field<Integer> F_STAR_RATING = DSL.field(DSL.name(TABLE, "star_rating"), Integer.class);
    private static final Field<Integer> F_TOTAL_ROOMS = DSL.field(DSL.name(TABLE, "total_rooms"), Integer.class);
    private static final Field<Integer> F_AVAILABLE_ROOMS = DSL.field(DSL.name(TABLE, "available_rooms"), Integer.class);
    private static final Field<Integer> F_MAX_GUESTS = DSL.field(DSL.name(TABLE, "max_guests"), Integer.class);
    private static final Field<LocalDateTime> F_CREATED_AT = DSL.field(DSL.name(TABLE, "created_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> F_UPDATED_AT = DSL.field(DSL.name(TABLE, "updated_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> F_DELETED_AT = DSL.field(DSL.name(TABLE, "deleted_at"), LocalDateTime.class);
    private static final Field<LocalTime> F_CHECK_IN = DSL.field(DSL.name(TABLE, "check_in_time"), LocalTime.class);
    private static final Field<LocalTime> F_CHECK_OUT = DSL.field(DSL.name(TABLE, "check_out_time"), LocalTime.class);

    private final DSLContext dsl;

    public Optional<ServiceResponse> findById(UUID id) {
        var record = dsl.select(F_ID, F_PROVIDER_ID, F_TYPE, F_NAME, F_DESCRIPTION, F_CITY, F_COUNTRY, F_ADDRESS,
                        F_BASE_PRICE, F_CURRENCY, F_STATUS, F_STAR_RATING, F_TOTAL_ROOMS, F_AVAILABLE_ROOMS, F_MAX_GUESTS,
                        F_CREATED_AT, F_UPDATED_AT, F_CHECK_IN, F_CHECK_OUT)
                .from(DSL.table(DSL.name(TABLE)))
                .where(F_ID.eq(id).and(F_DELETED_AT.isNull()))
                .fetchOne();
        return Optional.ofNullable(record).map(this::toResponse);
    }

    public org.springframework.data.domain.Page<ServiceResponse> findPage(int page, int size,
                                                                            UUID providerId,
                                                                            ServiceType type,
                                                                            ServiceStatus status,
                                                                            String city,
                                                                            String country) {
        int offset = page * size;
        Condition condition = F_DELETED_AT.isNull();
        if (providerId != null) condition = condition.and(F_PROVIDER_ID.eq(providerId));
        if (type != null) condition = condition.and(F_TYPE.eq(type.name()));
        if (status != null) condition = condition.and(F_STATUS.eq(status.name()));
        if (city != null && !city.isBlank()) condition = condition.and(F_CITY.equalIgnoreCase(city.trim()));
        if (country != null && !country.isBlank()) condition = condition.and(F_COUNTRY.equalIgnoreCase(country.trim()));

        var table = DSL.table(DSL.name(TABLE));
        long total = dsl.fetchCount(dsl.selectFrom(table).where(condition));
        List<ServiceResponse> content = dsl.select(F_ID, F_PROVIDER_ID, F_TYPE, F_NAME, F_DESCRIPTION, F_CITY, F_COUNTRY, F_ADDRESS,
                        F_BASE_PRICE, F_CURRENCY, F_STATUS, F_STAR_RATING, F_TOTAL_ROOMS, F_AVAILABLE_ROOMS, F_MAX_GUESTS,
                        F_CREATED_AT, F_UPDATED_AT, F_CHECK_IN, F_CHECK_OUT)
                .from(table)
                .where(condition)
                .orderBy(F_CREATED_AT.desc())
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

    public org.springframework.data.domain.Page<ServiceResponse> search(int page, int size,
                                                                         String q,
                                                                         ServiceType type,
                                                                         String city,
                                                                         BigDecimal minPrice,
                                                                         BigDecimal maxPrice) {
        int offset = page * size;
        Condition condition = F_DELETED_AT.isNull();
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim() + "%";
            condition = condition.and(DSL.or(
                    DSL.upper(F_NAME).like(DSL.upper(DSL.val(pattern))),
                    DSL.upper(F_DESCRIPTION).like(DSL.upper(DSL.val(pattern)))
            ));
        }
        if (type != null) condition = condition.and(F_TYPE.eq(type.name()));
        if (city != null && !city.isBlank()) condition = condition.and(F_CITY.equalIgnoreCase(city.trim()));
        if (minPrice != null) condition = condition.and(F_BASE_PRICE.greaterOrEqual(minPrice));
        if (maxPrice != null) condition = condition.and(F_BASE_PRICE.lessOrEqual(maxPrice));

        var table = DSL.table(DSL.name(TABLE));
        long total = dsl.fetchCount(dsl.selectFrom(table).where(condition));
        List<ServiceResponse> content = dsl.select(F_ID, F_PROVIDER_ID, F_TYPE, F_NAME, F_DESCRIPTION, F_CITY, F_COUNTRY, F_ADDRESS,
                        F_BASE_PRICE, F_CURRENCY, F_STATUS, F_STAR_RATING, F_TOTAL_ROOMS, F_AVAILABLE_ROOMS, F_MAX_GUESTS,
                        F_CREATED_AT, F_UPDATED_AT, F_CHECK_IN, F_CHECK_OUT)
                .from(table)
                .where(condition)
                .orderBy(F_BASE_PRICE.asc())
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

    private ServiceResponse toResponse(Record r) {
        return ServiceResponse.builder()
                .id(r.get(F_ID))
                .providerId(r.get(F_PROVIDER_ID))
                .type(parseType(r.get(F_TYPE)))
                .name(RequestLocaleHolder.localized(r.get(F_NAME), null))
                .description(RequestLocaleHolder.localized(r.get(F_DESCRIPTION), null))
                .city(r.get(F_CITY))
                .country(r.get(F_COUNTRY))
                .address(r.get(F_ADDRESS))
                .basePrice(r.get(F_BASE_PRICE))
                .currency(r.get(F_CURRENCY))
                .status(parseStatus(r.get(F_STATUS)))
                .starRating(r.get(F_STAR_RATING))
                .totalRooms(r.get(F_TOTAL_ROOMS))
                .availableRooms(r.get(F_AVAILABLE_ROOMS))
                .maxGuests(r.get(F_MAX_GUESTS) != null ? r.get(F_MAX_GUESTS) : 1)
                .checkInTime(r.get(F_CHECK_IN))
                .checkOutTime(r.get(F_CHECK_OUT))
                .createdAt(r.get(F_CREATED_AT))
                .updatedAt(r.get(F_UPDATED_AT))
                .build();
    }

    private static ServiceType parseType(Object v) {
        if (v == null) return null;
        try {
            return ServiceType.valueOf(v.toString().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ServiceStatus parseStatus(Object v) {
        if (v == null) return null;
        try {
            return ServiceStatus.valueOf(v.toString().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
