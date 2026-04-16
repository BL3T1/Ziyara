package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.Complaint;
import com.ziyara.backend.domain.enums.ComplaintPriority;
import com.ziyara.backend.domain.enums.ComplaintStatus;
import com.ziyara.backend.domain.repository.ComplaintRepository;
import com.ziyara.backend.infrastructure.persistence.entity.ComplaintJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.ComplaintMapper;
import com.ziyara.backend.infrastructure.persistence.repository.ComplaintJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ComplaintRepositoryAdapter implements ComplaintRepository {

    private final ComplaintJpaRepository jpaRepository;
    private final ComplaintMapper mapper;

    @Override
    public Complaint save(Complaint complaint) {
        ComplaintJpaEntity e = mapper.toJpa(complaint);
        return mapper.toDomain(jpaRepository.save(e));
    }

    @Override
    public Optional<Complaint> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Complaint> findByTicketNumber(String ticketNumber) {
        return jpaRepository.findByTicketNumber(ticketNumber).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void delete(Complaint complaint) {
        jpaRepository.deleteById(complaint.getId());
    }

    @Override
    public List<Complaint> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findByCustomerId(UUID customerId) {
        return jpaRepository.findByCustomerId(customerId).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findByBookingId(UUID bookingId) {
        return jpaRepository.findByBookingId(bookingId).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findByAssignedAgentId(UUID agentId) {
        return jpaRepository.findByAssignedAgentId(agentId).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findByStatus(ComplaintStatus status) {
        return jpaRepository.findByStatus(status).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findByPriority(ComplaintPriority priority) {
        return jpaRepository.findByPriority(priority).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findByStatusIn(List<ComplaintStatus> statuses) {
        return jpaRepository.findByStatusIn(statuses).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findByCustomerIdAndStatus(UUID customerId, ComplaintStatus status) {
        return jpaRepository.findByCustomerIdAndStatus(customerId, status).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findByAssignedAgentIdAndStatus(UUID agentId, ComplaintStatus status) {
        return jpaRepository.findByAssignedAgentIdAndStatus(agentId, status).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findByCreatedAtAfter(LocalDateTime date) {
        return jpaRepository.findByCreatedAtAfter(date).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByCreatedAtBetween(start, end).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findByResolvedAtBetween(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByResolvedAtBetween(start, end).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findOpenComplaints() {
        return jpaRepository.findOpenComplaints().stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findUnassignedComplaints() {
        return jpaRepository.findUnassignedComplaints().stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findOverdueComplaints(int hoursThreshold) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(hoursThreshold);
        return jpaRepository.findOverdueComplaints(threshold).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countOpenComplaints() {
        return jpaRepository.countOpenComplaints();
    }

    @Override
    public long countByStatus(ComplaintStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long countByPriority(ComplaintPriority priority) {
        return jpaRepository.countByPriority(priority);
    }

    @Override
    public long countByAssignedAgentId(UUID agentId) {
        return jpaRepository.countByAssignedAgentId(agentId);
    }

    @Override
    public long countByCustomerId(UUID customerId) {
        return jpaRepository.countByCustomerId(customerId);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByTicketNumber(String ticketNumber) {
        return jpaRepository.existsByTicketNumber(ticketNumber);
    }
}
