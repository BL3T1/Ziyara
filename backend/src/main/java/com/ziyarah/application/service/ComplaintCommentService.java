package com.ziyarah.application.service;

import com.ziyarah.application.dto.request.CreateComplaintCommentRequest;
import com.ziyarah.application.dto.response.ComplaintCommentResponse;
import com.ziyarah.domain.entity.ComplaintComment;
import com.ziyarah.domain.repository.ComplaintCommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ComplaintCommentService {
    
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
