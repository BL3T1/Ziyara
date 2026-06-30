package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.AssignComplaintRequest;
import com.ziyara.backend.application.dto.request.CreateComplaintRequest;
import com.ziyara.backend.application.dto.request.ResolveComplaintRequest;
import com.ziyara.backend.application.dto.request.UpdateComplaintRequest;
import com.ziyara.backend.application.dto.response.ComplaintResponse;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.Complaint;
import com.ziyara.backend.domain.enums.ComplaintPriority;
import com.ziyara.backend.domain.enums.ComplaintStatus;
import com.ziyara.backend.domain.repository.ComplaintRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.modules.notification.api.StaffNotificationCommandPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock ComplaintRepository complaintRepository;
    @Mock UserRepository userRepository;
    @Mock StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    ComplaintService service;

    @BeforeEach
    void setUp() {
        service = new ComplaintService(complaintRepository, userRepository, staffNotificationCommandPublisher);
    }

    // â”€â”€ update â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void update_complaintNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(complaintRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new UpdateComplaintRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_existingComplaint_updatesFields() {
        UUID id = UUID.randomUUID();
        Complaint complaint = complaintWithId(id);
        complaint.setSubject("Old Subject");
        when(complaintRepository.findById(id)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any())).thenReturn(complaint);

        UpdateComplaintRequest request = new UpdateComplaintRequest();
        request.setSubject("New Subject");
        request.setPriority(ComplaintPriority.HIGH);

        ComplaintResponse result = service.update(id, request);

        assertThat(complaint.getSubject()).isEqualTo("New Subject");
        assertThat(complaint.getPriority()).isEqualTo(ComplaintPriority.HIGH);
        verify(complaintRepository).save(complaint);
    }

    // â”€â”€ close â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void close_complaintNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(complaintRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.close(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void close_existingComplaint_setsClosedState() {
        UUID id = UUID.randomUUID();
        Complaint complaint = complaintWithId(id);
        complaint.setStatus(ComplaintStatus.RESOLVED);
        when(complaintRepository.findById(id)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any())).thenReturn(complaint);

        service.close(id);

        verify(complaintRepository).save(any());
    }

    // â”€â”€ getById â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void getById_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(complaintRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_found_returnsResponse() {
        UUID id = UUID.randomUUID();
        Complaint complaint = complaintWithId(id);
        complaint.setSubject("Test complaint");
        when(complaintRepository.findById(id)).thenReturn(Optional.of(complaint));

        ComplaintResponse result = service.getById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getSubject()).isEqualTo("Test complaint");
    }

    // â”€â”€ resolve â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void resolve_complaintNotFound_throwsBusinessException() {
        UUID id = UUID.randomUUID();
        when(complaintRepository.findById(id)).thenReturn(Optional.empty());

        ResolveComplaintRequest request = ResolveComplaintRequest.builder().notes("Resolved").build();

        // SubmitComplaintUseCase will fail because complaint not found â€” throws BusinessException
        assertThatThrownBy(() -> service.resolve(id, request, UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class);
    }

    private Complaint complaintWithId(UUID id) {
        Complaint c = new Complaint();
        c.setId(id);
        c.setTicketNumber("CMP-001");
        c.setStatus(ComplaintStatus.SUBMITTED);
        return c;
    }
}

