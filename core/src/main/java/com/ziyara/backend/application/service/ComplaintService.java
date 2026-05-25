package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.*;
import com.ziyara.backend.application.dto.response.ComplaintResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.Complaint;
import com.ziyara.backend.domain.enums.ComplaintPriority;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.repository.ComplaintRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.domain.usecase.complaint.AssignComplaintUseCase;
import com.ziyara.backend.domain.usecase.complaint.EscalateComplaintUseCase;
import com.ziyara.backend.domain.usecase.complaint.ResolveComplaintUseCase;
import com.ziyara.backend.domain.usecase.complaint.SubmitComplaintUseCase;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service: ComplaintService (Phase 2 â€“ Commands)
 * Handles complaint create, update, assign, resolve, escalate, close.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    @Transactional
    public ComplaintResponse create(CreateComplaintRequest request, UUID createdBy) {
        var result = new SubmitComplaintUseCase(complaintRepository, userRepository).execute(
                new SubmitComplaintUseCase.Input(
                        request.getCustomerId(), request.getBookingId(),
                        request.getSubject(), request.getDescription(),
                        request.getCategory(), request.getPriority()));
        if (!result.success()) throw new BusinessException(result.error());
        Complaint saved = result.complaint();
        log.info("Complaint created: {} by {}", saved.getId(), createdBy);
        staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .notificationType(NotificationType.COMPLAINT_NEW.name())
                .title("New complaint")
                .message("Ticket " + saved.getTicketNumber() + ": " + saved.getSubject())
                .notifyRoles(List.of("SUPPORT_MANAGER", "GENERAL_MANAGER", "CEO", "SALES_MANAGER"))
                .metadata("{\"complaintId\":\"" + saved.getId() + "\"}")
                .build());
        return toResponse(saved);
    }

    @Transactional
    public ComplaintResponse update(UUID id, UpdateComplaintRequest request) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        if (request.getSubject() != null) complaint.setSubject(request.getSubject());
        if (request.getDescription() != null) complaint.setDescription(request.getDescription());
        if (request.getPriority() != null) complaint.setPriority(request.getPriority());
        if (request.getCategory() != null) complaint.setCategory(request.getCategory());
        Complaint saved = complaintRepository.save(complaint);
        return toResponse(saved);
    }

    @Transactional
    public ComplaintResponse assign(UUID id, AssignComplaintRequest request) {
        var result = new AssignComplaintUseCase(complaintRepository, userRepository)
                .execute(new AssignComplaintUseCase.Input(id, request.getAgentId()));
        if (!result.success()) throw new BusinessException(result.error());
        Complaint saved = result.complaint();
        if (request.getAgentId() != null) {
            staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                    .eventId(UUID.randomUUID())
                    .notificationType(NotificationType.COMPLAINT_UPDATE.name())
                    .title("Complaint assigned")
                    .message("You were assigned complaint " + saved.getTicketNumber())
                    .recipientUserId(request.getAgentId())
                    .metadata("{\"complaintId\":\"" + saved.getId() + "\"}")
                    .build());
        }
        return toResponse(saved);
    }

    @Transactional
    public ComplaintResponse resolve(UUID id, ResolveComplaintRequest request, UUID resolvedBy) {
        var result = new ResolveComplaintUseCase(complaintRepository).execute(
                new ResolveComplaintUseCase.Input(id, resolvedBy,
                        request != null && request.getNotes() != null ? request.getNotes() : ""));
        if (!result.success()) throw new BusinessException(result.error());
        return toResponse(result.complaint());
    }

    @Transactional
    public ComplaintResponse escalate(UUID id, EscalateComplaintRequest request) {
        var result = new EscalateComplaintUseCase(complaintRepository)
                .execute(new EscalateComplaintUseCase.Input(id, request.getEscalateToId()));
        if (!result.success()) throw new BusinessException(result.error());
        return toResponse(result.complaint());
    }

    @Transactional
    public ComplaintResponse close(UUID id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        complaint.close();
        return toResponse(complaintRepository.save(complaint));
    }

    public ComplaintResponse getById(UUID id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        return toResponse(complaint);
    }

    private static ComplaintResponse toResponse(Complaint c) {
        return ComplaintResponse.builder()
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
