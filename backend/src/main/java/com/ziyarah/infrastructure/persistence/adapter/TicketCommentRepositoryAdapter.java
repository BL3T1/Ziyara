package com.ziyarah.infrastructure.persistence.adapter;

import com.ziyarah.domain.entity.TicketComment;
import com.ziyarah.domain.repository.TicketCommentRepository;
import com.ziyarah.infrastructure.persistence.entity.TicketCommentJpaEntity;
import com.ziyarah.infrastructure.persistence.mapper.TicketCommentMapper;
import com.ziyarah.infrastructure.persistence.repository.TicketCommentJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing TicketCommentRepository using JPA
 */
@Component
public class TicketCommentRepositoryAdapter implements TicketCommentRepository {
    
    private final TicketCommentJpaRepository jpaRepository;
    private final TicketCommentMapper mapper;
    
    public TicketCommentRepositoryAdapter(TicketCommentJpaRepository jpaRepository, 
                                          TicketCommentMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    @Transactional
    public TicketComment save(TicketComment comment) {
        TicketCommentJpaEntity jpaEntity = mapper.toJpaEntity(comment);
        TicketCommentJpaEntity savedEntity = jpaRepository.save(jpaEntity);
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    public Optional<TicketComment> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }
    
    @Override
    public List<TicketComment> findAll() {
        return mapper.toDomainList(jpaRepository.findAll());
    }
    
    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    public List<TicketComment> findByTicketId(UUID ticketId) {
        return mapper.toDomainList(jpaRepository.findByTicketId(ticketId));
    }
    
    @Override
    public List<TicketComment> findByTicketIdOrderByCreatedAtDesc(UUID ticketId) {
        return mapper.toDomainList(jpaRepository.findByTicketIdOrderByCreatedAtDesc(ticketId));
    }
    
    @Override
    public List<TicketComment> findByUserId(UUID userId) {
        return mapper.toDomainList(jpaRepository.findByUserId(userId));
    }
    
    @Override
    public List<TicketComment> findByTicketIdAndIsResolutionTrue(UUID ticketId) {
        return mapper.toDomainList(jpaRepository.findByTicketIdAndIsResolutionTrue(ticketId));
    }
    
    @Override
    public List<TicketComment> findByTicketIdAndIsInternalTrue(UUID ticketId) {
        return mapper.toDomainList(jpaRepository.findByTicketIdAndIsInternalTrue(ticketId));
    }
    
    @Override
    public List<TicketComment> findByTicketIdAndIsInternalFalse(UUID ticketId) {
        return mapper.toDomainList(jpaRepository.findByTicketIdAndIsInternalFalse(ticketId));
    }
    
    @Override
    public long count() {
        return jpaRepository.count();
    }
    
    @Override
    public long countByTicketId(UUID ticketId) {
        return jpaRepository.countByTicketId(ticketId);
    }
    
    @Override
    @Transactional
    public void deleteByTicketId(UUID ticketId) {
        jpaRepository.deleteByTicketId(ticketId);
    }
}
