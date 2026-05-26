package com.ziyara.backend.application.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCommentResponse {

    private UUID id;
    private UUID ticketId;
    private UUID userId;
    private String comment;
    private boolean internal;
    private boolean resolution;
    private String attachments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
