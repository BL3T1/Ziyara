package com.ziyara.backend.application.dto;

import com.ziyara.backend.domain.enums.TicketPriority;
import com.ziyara.backend.domain.enums.TicketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for creating/updating internal tickets
 */
public class TicketRequest {
    
    private TicketType type;
    private String subject;
    private String description;
    private TicketPriority priority;
    private String module;
    private String subModule;
    private String environment;
    private String browser;
    private String operatingSystem;
    private String stepsToReproduce;
    private String expectedBehavior;
    private String actualBehavior;
    private String attachments;
    private UUID assignedToId;
    private BigDecimal estimatedHours;
    private LocalDateTime dueDate;
    private UUID relatedTicketId;

    // Getters and Setters
    public TicketType getType() { return type; }
    public void setType(TicketType type) { this.type = type; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getSubModule() { return subModule; }
    public void setSubModule(String subModule) { this.subModule = subModule; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }
    public String getStepsToReproduce() { return stepsToReproduce; }
    public void setStepsToReproduce(String stepsToReproduce) { this.stepsToReproduce = stepsToReproduce; }
    public String getExpectedBehavior() { return expectedBehavior; }
    public void setExpectedBehavior(String expectedBehavior) { this.expectedBehavior = expectedBehavior; }
    public String getActualBehavior() { return actualBehavior; }
    public void setActualBehavior(String actualBehavior) { this.actualBehavior = actualBehavior; }
    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }
    public UUID getAssignedToId() { return assignedToId; }
    public void setAssignedToId(UUID assignedToId) { this.assignedToId = assignedToId; }
    public BigDecimal getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(BigDecimal estimatedHours) { this.estimatedHours = estimatedHours; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public UUID getRelatedTicketId() { return relatedTicketId; }
    public void setRelatedTicketId(UUID relatedTicketId) { this.relatedTicketId = relatedTicketId; }
}
