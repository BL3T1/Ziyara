package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.*;
import com.ziyara.backend.application.dto.response.ComplaintResponse;
import com.ziyara.backend.domain.entity.Complaint;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.enums.ComplaintPriority;
import com.ziyara.backend.domain.repository.ComplaintRepository;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationEvent;
import com.ziyara.backend.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service: ComplaintService (Phase 2 – Commands)
 * Handles complaint create, update, assign, resolve, escalate, close.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    private static final String TICKET_PREFIX = "CMP-";

    @Transactional
    public ComplaintResponse create(CreateComplaintRequest request, UUID createdBy) {
        String ticketNumber = generateUniqueTicketNumber();
        Complaint complaint = new Complaint(
                request.getCustomerId(),
                request.getSubject(),
                request.getDescription()
        );
        complaint.setBookingId(request.getBookingId());
        if (request.getPriority() != null) {
            complaint.setPriority(request.getPriority());
        }
        if (request.getCategory() != null) {
            complaint.setCategory(request.getCategory());
        }
        complaint.setTicketNumber(ticketNumber);
        Complaint saved = complaintRepository.save(complaint);
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
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        complaint.assign(request.getAgentId());
        Complaint saved = complaintRepository.save(complaint);
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
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        complaint.resolve(resolvedBy, request != null && request.getNotes() != null ? request.getNotes() : "");
        return toResponse(complaintRepository.save(complaint));
    }

    @Transactional
    public ComplaintResponse escalate(UUID id, EscalateComplaintRequest request) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        complaint.escalate(request.getEscalateToId());
        return toResponse(complaintRepository.save(complaint));
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

    private String generateUniqueTicketNumber() {
        String base = TICKET_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        if (complaintRepository.existsByTicketNumber(base)) {
            return base + "-" + (int) (Math.random() * 1000);
        }
        return base;
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
