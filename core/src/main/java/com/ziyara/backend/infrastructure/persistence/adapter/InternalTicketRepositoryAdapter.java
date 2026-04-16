package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.InternalTicket;
import com.ziyara.backend.domain.enums.TicketStatus;
import com.ziyara.backend.domain.enums.TicketPriority;
import com.ziyara.backend.domain.enums.TicketType;
import com.ziyara.backend.domain.repository.InternalTicketRepository;
import com.ziyara.backend.infrastructure.persistence.entity.InternalTicketJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.InternalTicketMapper;
import com.ziyara.backend.infrastructure.persistence.repository.InternalTicketJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter implementing InternalTicketRepository using JPA
 */
@Component
public class InternalTicketRepositoryAdapter implements InternalTicketRepository {
    
    private final InternalTicketJpaRepository jpaRepository;
    private final InternalTicketMapper mapper;
    
    public InternalTicketRepositoryAdapter(InternalTicketJpaRepository jpaRepository, 
                                           InternalTicketMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    @Transactional
    public InternalTicket save(InternalTicket ticket) {
        InternalTicketJpaEntity jpaEntity = mapper.toJpaEntity(ticket);
        InternalTicketJpaEntity savedEntity = jpaRepository.save(jpaEntity);
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    public Optional<InternalTicket> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }
    
    @Override
    public Optional<InternalTicket> findByTicketNumber(String ticketNumber) {
        return jpaRepository.findByTicketNumber(ticketNumber)
                .map(mapper::toDomain);
    }
    
    @Override
    public List<InternalTicket> findAll() {
        return mapper.toDomainList(jpaRepository.findAll());
    }
    
    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    public List<InternalTicket> findByReporterId(UUID reporterId) {
        return mapper.toDomainList(jpaRepository.findByReporterId(reporterId));
    }
    
    @Override
    public List<InternalTicket> findByAssignedToId(UUID assignedToId) {
        return mapper.toDomainList(jpaRepository.findByAssignedToId(assignedToId));
    }
    
    @Override
    public List<InternalTicket> findByStatus(TicketStatus status) {
        return mapper.toDomainList(jpaRepository.findByStatus(status));
    }
    
    @Override
    public List<InternalTicket> findByStatusIn(List<TicketStatus> statuses) {
        return mapper.toDomainList(jpaRepository.findByStatusIn(statuses));
    }
    
    @Override
    public List<InternalTicket> findByType(TicketType type) {
        return mapper.toDomainList(jpaRepository.findByType(type));
    }
    
    @Override
    public List<InternalTicket> findByPriority(TicketPriority priority) {
        return mapper.toDomainList(jpaRepository.findByPriority(priority));
    }
    
    @Override
    public List<InternalTicket> findByModule(String module) {
        return mapper.toDomainList(jpaRepository.findByModule(module));
    }
    
    @Override
    public List<InternalTicket> findOverdueTickets(LocalDateTime currentDate) {
        List<TicketStatus> closedStatuses = Arrays.asList(
            TicketStatus.CLOSED, 
            TicketStatus.CANCELLED, 
            TicketStatus.RESOLVED, 
            TicketStatus.VERIFIED
        );
        return mapper.toDomainList(jpaRepository.findOverdueTickets(currentDate, closedStatuses));
    }
    
    @Override
    public List<InternalTicket> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        return mapper.toDomainList(jpaRepository.findByCreatedAtBetween(start, end));
    }
    
    @Override
    public List<InternalTicket> findByRelatedTicketId(UUID relatedTicketId) {
        return mapper.toDomainList(jpaRepository.findByRelatedTicketId(relatedTicketId));
    }
    
    @Override
    public long count() {
        return jpaRepository.count();
    }
    
    @Override
    public long countByStatus(TicketStatus status) {
        return jpaRepository.countByStatus(status);
    }
    
    @Override
    public long countByType(TicketType type) {
        return jpaRepository.countByType(type);
    }
    
    @Override
    public long countByPriority(TicketPriority priority) {
        return jpaRepository.countByPriority(priority);
    }
    
    @Override
    public long countByAssignedToId(UUID assignedToId) {
        return jpaRepository.countByAssignedToId(assignedToId);
    }
    
    @Override
    public long countOverdueTickets(LocalDateTime currentDate) {
        List<TicketStatus> closedStatuses = Arrays.asList(
            TicketStatus.CLOSED, 
            TicketStatus.CANCELLED, 
            TicketStatus.RESOLVED, 
            TicketStatus.VERIFIED
        );
        return jpaRepository.countOverdueTickets(currentDate, closedStatuses);
    }
    
    @Override
    public long countByStatusIn(List<TicketStatus> statuses) {
        return jpaRepository.countByStatusIn(statuses);
    }
    
    @Override
    public List<InternalTicket> search(String keyword) {
        return mapper.toDomainList(jpaRepository.search(keyword));
    }
}
