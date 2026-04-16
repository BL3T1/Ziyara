package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateComplaintCommentRequest;
import com.ziyara.backend.application.dto.response.ComplaintCommentResponse;
import com.ziyara.backend.domain.entity.ComplaintComment;
import com.ziyara.backend.domain.repository.ComplaintCommentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service: ComplaintCommentService
 * Handles internal and external support communication
 */
@Service
@RequiredArgsConstructor
public class ComplaintCommentService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintCommentService.class);
    
    private final ComplaintCommentRepository commentRepository;
    
    @Transactional
    public ComplaintCommentResponse addComment(UUID complaintId, UUID userId, CreateComplaintCommentRequest request) {
        log.info("Adding comment to complaint: {} by user: {}", complaintId, userId);
        
        ComplaintComment comment = new ComplaintComment();
        comment.setComplaintId(complaintId);
        comment.setUserId(userId);
        comment.setComment(request.getComment());
        comment.setInternal(request.getIsInternal() != null && request.getIsInternal());
        
        return mapToResponse(commentRepository.save(comment));
    }
    
    @Transactional(readOnly = true)
    public List<ComplaintCommentResponse> getComplaintComments(UUID complaintId, boolean includeInternal) {
        List<ComplaintComment> comments;
        if (includeInternal) {
            comments = commentRepository.findByComplaintId(complaintId);
        } else {
            comments = commentRepository.findByComplaintIdAndIsInternal(complaintId, false);
        }
        
        return comments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private ComplaintCommentResponse mapToResponse(ComplaintComment comment) {
        return ComplaintCommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .comment(comment.getComment())
                .isInternal(comment.isInternal())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
