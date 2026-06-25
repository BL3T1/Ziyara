package com.ziyara.backend.application.service;

import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.Customer;
import com.ziyara.backend.domain.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityDocumentService {

    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public IdentityStatus getStatus(UUID userId) {
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (customer.isIdentityVerified()) {
            return new IdentityStatus("VERIFIED", customer.getIdDocumentUrl());
        } else if (customer.getIdDocumentUrl() != null && !customer.getIdDocumentUrl().isBlank()) {
            return new IdentityStatus("PENDING", customer.getIdDocumentUrl());
        } else {
            return new IdentityStatus("NONE", null);
        }
    }

    @Transactional
    public void uploadDocument(UUID userId, String documentUrl) {
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        customer.setIdDocumentUrl(documentUrl);
        customer.setIdentityVerified(false);
        customerRepository.save(customer);
        log.info("Identity document uploaded for user {}", userId);
    }

    @Transactional
    public void verify(UUID userId, boolean approved, UUID reviewerId, String reason) {
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (customer.getIdDocumentUrl() == null || customer.getIdDocumentUrl().isBlank()) {
            throw new BusinessException("No identity document uploaded");
        }

        customer.setIdentityVerified(approved);
        customer.setIdentityDocumentReviewedAt(LocalDateTime.now());
        customer.setIdentityDocumentReviewedBy(reviewerId);
        if (!approved) {
            customer.setIdDocumentUrl(null);
        }
        customerRepository.save(customer);

        String action = approved ? "IDENTITY_VERIFY_APPROVE" : "IDENTITY_VERIFY_REJECT";
        auditLogService.logAction(action, "Customer", userId.toString(), reviewerId,
                null, reason, null, null);
        log.info("Identity verification {} for user {} by {}", approved ? "approved" : "rejected",
                userId, reviewerId);
    }

    @Transactional(readOnly = true)
    public List<IdentityVerificationEntry> listVerifications(String statusFilter) {
        List<Customer> customers;
        if ("VERIFIED".equalsIgnoreCase(statusFilter)) {
            customers = customerRepository.findVerifiedIdentityDocuments();
        } else if ("PENDING".equalsIgnoreCase(statusFilter)) {
            customers = customerRepository.findPendingIdentityVerifications();
        } else {
            List<Customer> all = new ArrayList<>();
            all.addAll(customerRepository.findPendingIdentityVerifications());
            all.addAll(customerRepository.findVerifiedIdentityDocuments());
            customers = all;
        }
        return customers.stream()
                .map(c -> new IdentityVerificationEntry(
                        c.getUserId(), c.getFirstName(), c.getLastName(),
                        c.isIdentityVerified() ? "VERIFIED" : "PENDING",
                        c.getIdDocumentUrl(),
                        c.getIdentityDocumentReviewedAt(),
                        c.getIdentityDocumentReviewedBy()))
                .toList();
    }

    public record IdentityStatus(String status, String documentUrl) {}

    public record IdentityVerificationEntry(
            UUID userId,
            String firstName,
            String lastName,
            String status,
            String documentUrl,
            LocalDateTime reviewedAt,
            UUID reviewedBy) {}
}
