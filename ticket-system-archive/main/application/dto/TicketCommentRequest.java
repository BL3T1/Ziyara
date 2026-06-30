package com.ziyara.backend.application.dto;

import java.util.UUID;

/**
 * DTO for creating ticket comments
 */
public class TicketCommentRequest {
    
    private UUID ticketId;
    private String comment;
    private boolean isInternal = true;
    private boolean isResolution = false;
    private String attachments;

    // Getters and Setters
    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public boolean isInternal() { return isInternal; }
    public void setInternal(boolean internal) { isInternal = internal; }
    public boolean isResolution() { return isResolution; }
    public void setResolution(boolean resolution) { isResolution = resolution; }
    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }
}
