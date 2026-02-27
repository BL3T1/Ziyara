package com.ziyarah.infrastructure.persistence.mapper;

import com.ziyarah.domain.entity.InternalTicket;
import com.ziyarah.infrastructure.persistence.entity.InternalTicketJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between InternalTicket domain entity and JPA entity
 */
@Component
public class InternalTicketMapper {
    
    public InternalTicket toDomain(InternalTicketJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }
        
        InternalTicket ticket = new InternalTicket();
        ticket.setId(jpaEntity.getId());
        ticket.setTicketNumber(jpaEntity.getTicketNumber());
        ticket.setReporterId(jpaEntity.getReporterId());
        ticket.setType(jpaEntity.getType());
        ticket.setSubject(jpaEntity.getSubject());
        ticket.setDescription(jpaEntity.getDescription());
        ticket.setPriority(jpaEntity.getPriority());
        ticket.setStatus(jpaEntity.getStatus());
        ticket.setModule(jpaEntity.getModule());
        ticket.setSubModule(jpaEntity.getSubModule());
        ticket.setEnvironment(jpaEntity.getEnvironment());
        ticket.setBrowser(jpaEntity.getBrowser());
        ticket.setOperatingSystem(jpaEntity.getOperatingSystem());
        ticket.setStepsToReproduce(jpaEntity.getStepsToReproduce());
        ticket.setExpectedBehavior(jpaEntity.getExpectedBehavior());
        ticket.setActualBehavior(jpaEntity.getActualBehavior());
        ticket.setAttachments(jpaEntity.getAttachments());
        ticket.setAssignedToId(jpaEntity.getAssignedToId());
        ticket.setAssignedAt(jpaEntity.getAssignedAt());
        ticket.setEstimatedHours(jpaEntity.getEstimatedHours());
        ticket.setActualHours(jpaEntity.getActualHours());
        ticket.setDueDate(jpaEntity.getDueDate());
        ticket.setResolutionNotes(jpaEntity.getResolutionNotes());
        ticket.setResolutionSummary(jpaEntity.getResolutionSummary());
        ticket.setResolvedAt(jpaEntity.getResolvedAt());
        ticket.setResolvedBy(jpaEntity.getResolvedBy());
        ticket.setVerifiedAt(jpaEntity.getVerifiedAt());
        ticket.setVerifiedBy(jpaEntity.getVerifiedBy());
        ticket.setClosedAt(jpaEntity.getClosedAt());
        ticket.setClosedBy(jpaEntity.getClosedBy());
        ticket.setCancelledAt(jpaEntity.getCancelledAt());
        ticket.setCancelledBy(jpaEntity.getCancelledBy());
        ticket.setCancellationReason(jpaEntity.getCancellationReason());
        ticket.setRelatedTicketId(jpaEntity.getRelatedTicketId());
        ticket.setCreatedAt(jpaEntity.getCreatedAt());
        ticket.setUpdatedAt(jpaEntity.getUpdatedAt());
        
        return ticket;
    }
    
    public InternalTicketJpaEntity toJpaEntity(InternalTicket ticket) {
        if (ticket == null) {
            return null;
        }
        
        InternalTicketJpaEntity jpaEntity = new InternalTicketJpaEntity();
        jpaEntity.setId(ticket.getId());
        jpaEntity.setTicketNumber(ticket.getTicketNumber());
        jpaEntity.setReporterId(ticket.getReporterId());
        jpaEntity.setType(ticket.getType());
        jpaEntity.setSubject(ticket.getSubject());
        jpaEntity.setDescription(ticket.getDescription());
        jpaEntity.setPriority(ticket.getPriority());
        jpaEntity.setStatus(ticket.getStatus());
        jpaEntity.setModule(ticket.getModule());
        jpaEntity.setSubModule(ticket.getSubModule());
        jpaEntity.setEnvironment(ticket.getEnvironment());
        jpaEntity.setBrowser(ticket.getBrowser());
        jpaEntity.setOperatingSystem(ticket.getOperatingSystem());
        jpaEntity.setStepsToReproduce(ticket.getStepsToReproduce());
        jpaEntity.setExpectedBehavior(ticket.getExpectedBehavior());
        jpaEntity.setActualBehavior(ticket.getActualBehavior());
        jpaEntity.setAttachments(ticket.getAttachments());
        jpaEntity.setAssignedToId(ticket.getAssignedToId());
        jpaEntity.setAssignedAt(ticket.getAssignedAt());
        jpaEntity.setEstimatedHours(ticket.getEstimatedHours());
        jpaEntity.setActualHours(ticket.getActualHours());
        jpaEntity.setDueDate(ticket.getDueDate());
        jpaEntity.setResolutionNotes(ticket.getResolutionNotes());
        jpaEntity.setResolutionSummary(ticket.getResolutionSummary());
        jpaEntity.setResolvedAt(ticket.getResolvedAt());
        jpaEntity.setResolvedBy(ticket.getResolvedBy());
        jpaEntity.setVerifiedAt(ticket.getVerifiedAt());
        jpaEntity.setVerifiedBy(ticket.getVerifiedBy());
        jpaEntity.setClosedAt(ticket.getClosedAt());
        jpaEntity.setClosedBy(ticket.getClosedBy());
        jpaEntity.setCancelledAt(ticket.getCancelledAt());
        jpaEntity.setCancelledBy(ticket.getCancelledBy());
        jpaEntity.setCancellationReason(ticket.getCancellationReason());
        jpaEntity.setRelatedTicketId(ticket.getRelatedTicketId());
        jpaEntity.setCreatedAt(ticket.getCreatedAt());
        jpaEntity.setUpdatedAt(ticket.getUpdatedAt());
        
        return jpaEntity;
    }
    
    public List<InternalTicket> toDomainList(List<InternalTicketJpaEntity> jpaEntities) {
        return jpaEntities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
}
