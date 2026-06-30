package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.Complaint;
import com.ziyara.backend.infrastructure.persistence.entity.ComplaintJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ComplaintMapper {

    public Complaint toDomain(ComplaintJpaEntity e) {
        if (e == null) return null;
        Complaint c = new Complaint();
        c.setId(e.getId());
        c.setTicketNumber(e.getTicketNumber());
        c.setCustomerId(e.getCustomerId());
        c.setBookingId(e.getBookingId());
        c.setSubject(e.getSubject());
        c.setDescription(e.getDescription());
        c.setPriority(e.getPriority());
        c.setStatus(e.getStatus());
        c.setCategory(e.getCategory());
        c.setAssignedAgentId(e.getAssignedAgentId());
        c.setAssignedAt(e.getAssignedAt());
        c.setResolutionNotes(e.getResolutionNotes());
        c.setResolvedAt(e.getResolvedAt());
        c.setResolvedBy(e.getResolvedBy());
        c.setEscalatedAt(e.getEscalatedAt());
        c.setEscalatedTo(e.getEscalatedTo());
        c.setClosedAt(e.getClosedAt());
        c.setCreatedAt(e.getCreatedAt());
        c.setUpdatedAt(e.getUpdatedAt());
        return c;
    }

    public ComplaintJpaEntity toJpa(Complaint c) {
        if (c == null) return null;
        return ComplaintJpaEntity.builder()
                .id(c.getId())
                .ticketNumber(c.getTicketNumber())
                .customerId(c.getCustomerId())
                .bookingId(c.getBookingId())
                .subject(c.getSubject())
                .description(c.getDescription())
                .priority(c.getPriority())
                .status(c.getStatus())
                .category(c.getCategory())
                .assignedAgentId(c.getAssignedAgentId())
                .assignedAt(c.getAssignedAt())
                .resolutionNotes(c.getResolutionNotes())
                .resolvedAt(c.getResolvedAt())
                .resolvedBy(c.getResolvedBy())
                .escalatedAt(c.getEscalatedAt())
                .escalatedTo(c.getEscalatedTo())
                .closedAt(c.getClosedAt())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
