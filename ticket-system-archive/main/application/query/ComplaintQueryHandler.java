package com.ziyara.backend.application.query;

import com.ziyara.backend.application.dto.response.ComplaintResponse;
import com.ziyara.backend.domain.enums.ComplaintPriority;
import com.ziyara.backend.domain.enums.ComplaintStatus;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Query handler for complaint reads (CQRS query side, jOOQ).
 * GET /complaints (list with filters), GET /complaints/{id}.
 */
@Component
@RequiredArgsConstructor
public class ComplaintQueryHandler {

    private static final String TABLE = "support_complaints";
    private static final Field<UUID> F_ID = DSL.field(DSL.name(TABLE, "id"), UUID.class);
    private static final Field<String> F_TICKET_NUMBER = DSL.field(DSL.name(TABLE, "ticket_number"), String.class);
    private static final Field<UUID> F_CUSTOMER_ID = DSL.field(DSL.name(TABLE, "customer_id"), UUID.class);
    private static final Field<UUID> F_BOOKING_ID = DSL.field(DSL.name(TABLE, "booking_id"), UUID.class);
    private static final Field<String> F_SUBJECT = DSL.field(DSL.name(TABLE, "subject"), String.class);
    private static final Field<String> F_DESCRIPTION = DSL.field(DSL.name(TABLE, "description"), String.class);
    private static final Field<String> F_PRIORITY = DSL.field(DSL.name(TABLE, "priority"), String.class);
    private static final Field<String> F_STATUS = DSL.field(DSL.name(TABLE, "status"), String.class);
    private static final Field<String> F_CATEGORY = DSL.field(DSL.name(TABLE, "category"), String.class);
    private static final Field<UUID> F_ASSIGNED_AGENT_ID = DSL.field(DSL.name(TABLE, "assigned_agent_id"), UUID.class);
    private static final Field<LocalDateTime> F_ASSIGNED_AT = DSL.field(DSL.name(TABLE, "assigned_at"), LocalDateTime.class);
    private static final Field<String> F_RESOLUTION_NOTES = DSL.field(DSL.name(TABLE, "resolution_notes"), String.class);
    private static final Field<LocalDateTime> F_RESOLVED_AT = DSL.field(DSL.name(TABLE, "resolved_at"), LocalDateTime.class);
    private static final Field<UUID> F_RESOLVED_BY = DSL.field(DSL.name(TABLE, "resolved_by"), UUID.class);
    private static final Field<LocalDateTime> F_ESCALATED_AT = DSL.field(DSL.name(TABLE, "escalated_at"), LocalDateTime.class);
    private static final Field<UUID> F_ESCALATED_TO = DSL.field(DSL.name(TABLE, "escalated_to"), UUID.class);
    private static final Field<LocalDateTime> F_CLOSED_AT = DSL.field(DSL.name(TABLE, "closed_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> F_CREATED_AT = DSL.field(DSL.name(TABLE, "created_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> F_UPDATED_AT = DSL.field(DSL.name(TABLE, "updated_at"), LocalDateTime.class);

    private final DSLContext dsl;

    public Optional<ComplaintResponse> findById(UUID id) {
        var record = dsl.select(F_ID, F_TICKET_NUMBER, F_CUSTOMER_ID, F_BOOKING_ID, F_SUBJECT, F_DESCRIPTION,
                        F_PRIORITY, F_STATUS, F_CATEGORY, F_ASSIGNED_AGENT_ID, F_ASSIGNED_AT, F_RESOLUTION_NOTES,
                        F_RESOLVED_AT, F_RESOLVED_BY, F_ESCALATED_AT, F_ESCALATED_TO, F_CLOSED_AT, F_CREATED_AT, F_UPDATED_AT)
                .from(DSL.table(DSL.name(TABLE)))
                .where(F_ID.eq(id))
                .fetchOne();
        return Optional.ofNullable(record).map(this::toResponse);
    }

    public org.springframework.data.domain.Page<ComplaintResponse> findPage(int page, int size,
                                                                            ComplaintStatus status,
                                                                            ComplaintPriority priority,
                                                                            UUID customerId,
                                                                            UUID assignedTo) {
        int offset = page * size;
        Condition condition = DSL.noCondition();
        if (status != null) {
            condition = condition.and(F_STATUS.eq(status.name()));
        }
        if (priority != null) {
            condition = condition.and(F_PRIORITY.eq(priority.name()));
        }
        if (customerId != null) {
            condition = condition.and(F_CUSTOMER_ID.eq(customerId));
        }
        if (assignedTo != null) {
            condition = condition.and(F_ASSIGNED_AGENT_ID.eq(assignedTo));
        }

        var table = DSL.table(DSL.name(TABLE));
        long total = dsl.fetchCount(dsl.selectFrom(table).where(condition));

        List<ComplaintResponse> content = dsl.select(F_ID, F_TICKET_NUMBER, F_CUSTOMER_ID, F_BOOKING_ID, F_SUBJECT, F_DESCRIPTION,
                        F_PRIORITY, F_STATUS, F_CATEGORY, F_ASSIGNED_AGENT_ID, F_ASSIGNED_AT, F_RESOLUTION_NOTES,
                        F_RESOLVED_AT, F_RESOLVED_BY, F_ESCALATED_AT, F_ESCALATED_TO, F_CLOSED_AT, F_CREATED_AT, F_UPDATED_AT)
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

    private ComplaintResponse toResponse(Record r) {
        return ComplaintResponse.builder()
                .id(r.get(F_ID))
                .ticketNumber(r.get(F_TICKET_NUMBER))
                .customerId(r.get(F_CUSTOMER_ID))
                .bookingId(r.get(F_BOOKING_ID))
                .subject(r.get(F_SUBJECT))
                .description(r.get(F_DESCRIPTION))
                .priority(parsePriority(r.get(F_PRIORITY)))
                .status(parseStatus(r.get(F_STATUS)))
                .category(r.get(F_CATEGORY))
                .assignedAgentId(r.get(F_ASSIGNED_AGENT_ID))
                .assignedAt(r.get(F_ASSIGNED_AT))
                .resolutionNotes(r.get(F_RESOLUTION_NOTES))
                .resolvedAt(r.get(F_RESOLVED_AT))
                .resolvedBy(r.get(F_RESOLVED_BY))
                .escalatedAt(r.get(F_ESCALATED_AT))
                .escalatedTo(r.get(F_ESCALATED_TO))
                .closedAt(r.get(F_CLOSED_AT))
                .createdAt(r.get(F_CREATED_AT))
                .updatedAt(r.get(F_UPDATED_AT))
                .build();
    }

    private static ComplaintStatus parseStatus(Object v) {
        if (v == null) return null;
        try {
            return ComplaintStatus.valueOf(v.toString().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ComplaintPriority parsePriority(Object v) {
        if (v == null) return null;
        try {
            return ComplaintPriority.valueOf(v.toString().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
