package com.ziyara.backend.domain.entity;

import java.time.Instant;
import java.util.UUID;

public class ContactLead {

    private UUID id;
    private String name;
    private String email;
    private String company;
    private String message;
    private String ipAddress;
    private Instant createdAt;

    public ContactLead() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
