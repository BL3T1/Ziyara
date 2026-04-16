package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.ComplaintComment;
import com.ziyara.backend.domain.repository.ComplaintCommentRepository;
import com.ziyara.backend.infrastructure.persistence.entity.ComplaintCommentJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.ComplaintCommentMapper;
import com.ziyara.backend.infrastructure.persistence.repository.ComplaintCommentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: ComplaintCommentRepositoryAdapter
 */
@Component
@RequiredArgsConstructor
public class ComplaintCommentRepositoryAdapter implements ComplaintCommentRepository {
    
    private final ComplaintCommentJpaRepository complaintCommentJpaRepository;
    private final ComplaintCommentMapper complaintCommentMapper;
    
    @Override
    public ComplaintComment save(ComplaintComment comment) {
        ComplaintCommentJpaEntity entity = complaintCommentMapper.toJpaEntity(comment);
        ComplaintCommentJpaEntity savedEntity = complaintCommentJpaRepository.save(entity);
        return complaintCommentMapper.toDomainEntity(savedEntity);
    }
    
    @Override
    public Optional<ComplaintComment> findById(UUID id) {
        return complaintCommentJpaRepository.findById(id)
                .map(complaintCommentMapper::toDomainEntity);
    }
    
    @Override
    public List<ComplaintComment> findByComplaintId(UUID complaintId) {
        return complaintCommentJpaRepository.findByComplaintId(complaintId).stream()
                .map(complaintCommentMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ComplaintComment> findByComplaintIdAndIsInternal(UUID complaintId, boolean isInternal) {
        return complaintCommentJpaRepository.findByComplaintIdAndIsInternal(complaintId, isInternal).stream()
                .map(complaintCommentMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(UUID id) {
        complaintCommentJpaRepository.deleteById(id);
    }
}
