package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.TicketRequest;
import com.ziyara.backend.application.dto.TicketResponse;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.InternalTicket;
import com.ziyara.backend.domain.enums.TicketPriority;
import com.ziyara.backend.domain.enums.TicketStatus;
import com.ziyara.backend.domain.enums.TicketType;
import com.ziyara.backend.domain.repository.InternalTicketRepository;
import com.ziyara.backend.domain.repository.TicketCommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalTicketServiceTest {

    @Mock InternalTicketRepository ticketRepository;
    @Mock TicketCommentRepository commentRepository;

    InternalTicketService service;

    @BeforeEach
    void setUp() {
        service = new InternalTicketService(ticketRepository, commentRepository);
    }

    // ── createTicket ──────────────────────────────────────────────────────────

    @Test
    void createTicket_noTypeOrPriority_defaultsToGeneralInquiryAndMedium() {
        UUID reporterId = UUID.randomUUID();
        TicketRequest request = new TicketRequest();
        request.setSubject("Bug in login");

        InternalTicket saved = ticket(UUID.randomUUID(), TicketStatus.SUBMITTED);
        saved.setType(TicketType.GENERAL_INQUIRY);
        saved.setPriority(TicketPriority.MEDIUM);
        when(ticketRepository.save(any())).thenReturn(saved);

        service.createTicket(request, reporterId);

        ArgumentCaptor<InternalTicket> captor = ArgumentCaptor.forClass(InternalTicket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(TicketType.GENERAL_INQUIRY);
        assertThat(captor.getValue().getPriority()).isEqualTo(TicketPriority.MEDIUM);
    }

    @Test
    void createTicket_explicitTypeAndPriority_usesProvided() {
        UUID reporterId = UUID.randomUUID();
        TicketRequest request = new TicketRequest();
        request.setSubject("Production outage");
        request.setType(TicketType.BUG_REPORT);
        request.setPriority(TicketPriority.CRITICAL);

        InternalTicket saved = ticket(UUID.randomUUID(), TicketStatus.SUBMITTED);
        saved.setType(TicketType.BUG_REPORT);
        saved.setPriority(TicketPriority.CRITICAL);
        when(ticketRepository.save(any())).thenReturn(saved);

        service.createTicket(request, reporterId);

        ArgumentCaptor<InternalTicket> captor = ArgumentCaptor.forClass(InternalTicket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(TicketType.BUG_REPORT);
        assertThat(captor.getValue().getPriority()).isEqualTo(TicketPriority.CRITICAL);
    }

    // ── listTickets ───────────────────────────────────────────────────────────

    @Test
    void listTickets_noFilters_returnsAll() {
        InternalTicket t = ticket(UUID.randomUUID(), TicketStatus.SUBMITTED);
        when(ticketRepository.findAll()).thenReturn(List.of(t));

        List<TicketResponse> result = service.listTickets(null, null, null, null, null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void listTickets_statusFilter_delegatesToFindByStatus() {
        InternalTicket t = ticket(UUID.randomUUID(), TicketStatus.IN_PROGRESS);
        when(ticketRepository.findByStatus(TicketStatus.IN_PROGRESS)).thenReturn(List.of(t));

        List<TicketResponse> result = service.listTickets(TicketStatus.IN_PROGRESS, null, null, null, null, null);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findByStatus(TicketStatus.IN_PROGRESS);
    }

    @Test
    void listTickets_searchFilter_delegatesToSearch() {
        InternalTicket t = ticket(UUID.randomUUID(), TicketStatus.SUBMITTED);
        when(ticketRepository.search("login bug")).thenReturn(List.of(t));

        List<TicketResponse> result = service.listTickets(null, null, null, null, null, "login bug");

        assertThat(result).hasSize(1);
        verify(ticketRepository).search("login bug");
    }

    @Test
    void listTickets_assignedToFilter_delegatesToFindByAssignedTo() {
        UUID assigneeId = UUID.randomUUID();
        when(ticketRepository.findByAssignedToId(assigneeId)).thenReturn(List.of());

        service.listTickets(null, null, null, null, assigneeId, null);

        verify(ticketRepository).findByAssignedToId(assigneeId);
    }

    // ── getTicket ─────────────────────────────────────────────────────────────

    @Test
    void getTicket_validUuid_loadsById() {
        UUID id = UUID.randomUUID();
        InternalTicket t = ticket(id, TicketStatus.SUBMITTED);
        t.setSubject("Lookup by UUID");
        when(ticketRepository.findById(id)).thenReturn(Optional.of(t));

        TicketResponse result = service.getTicket(id.toString());

        assertThat(result.getSubject()).isEqualTo("Lookup by UUID");
    }

    @Test
    void getTicket_ticketNumber_loadsFromTicketNumber() {
        InternalTicket t = ticket(UUID.randomUUID(), TicketStatus.SUBMITTED);
        t.setTicketNumber("TKT-001");
        when(ticketRepository.findByTicketNumber("TKT-001")).thenReturn(Optional.of(t));

        TicketResponse result = service.getTicket("TKT-001");

        assertThat(result.getTicketNumber()).isEqualTo("TKT-001");
    }

    @Test
    void getTicket_byUuid_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTicket(id.toString()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTicket_byNumber_notFound_throwsResourceNotFound() {
        when(ticketRepository.findByTicketNumber("TKT-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTicket("TKT-999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteTicket ──────────────────────────────────────────────────────────

    @Test
    void deleteTicket_delegatesToRepository() {
        UUID id = UUID.randomUUID();

        service.deleteTicket(id);

        verify(ticketRepository).deleteById(id);
    }

    // ── updateTicket ──────────────────────────────────────────────────────────

    @Test
    void updateTicket_partialUpdate_onlyModifiesNonNullFields() {
        UUID id = UUID.randomUUID();
        InternalTicket existing = ticket(id, TicketStatus.SUBMITTED);
        existing.setSubject("Old subject");
        existing.setPriority(TicketPriority.LOW);
        when(ticketRepository.findById(id)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(any())).thenReturn(existing);

        TicketRequest request = new TicketRequest();
        request.setSubject("New subject");

        service.updateTicket(id, request);

        assertThat(existing.getSubject()).isEqualTo("New subject");
        assertThat(existing.getPriority()).isEqualTo(TicketPriority.LOW);
    }

    private InternalTicket ticket(UUID id, TicketStatus status) {
        InternalTicket t = new InternalTicket();
        t.setId(id);
        t.setTicketNumber("TKT-" + id.toString().substring(0, 4));
        t.setStatus(status);
        t.setType(TicketType.GENERAL_INQUIRY);
        t.setPriority(TicketPriority.MEDIUM);
        t.setSubject("Test ticket");
        return t;
    }
}
