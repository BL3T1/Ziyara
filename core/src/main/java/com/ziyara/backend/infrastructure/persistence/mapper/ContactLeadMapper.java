package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.ContactLead;
import com.ziyara.backend.infrastructure.persistence.entity.ContactLeadJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ContactLeadMapper {

    public ContactLead toDomainEntity(ContactLeadJpaEntity entity) {
        if (entity == null) return null;
        ContactLead lead = new ContactLead();
        lead.setId(entity.getId());
        lead.setName(entity.getName());
        lead.setEmail(entity.getEmail());
        lead.setCompany(entity.getCompany());
        lead.setMessage(entity.getMessage());
        lead.setClientIp(entity.getClientIp());
        lead.setCreatedAt(entity.getCreatedAt());
        return lead;
    }

    public ContactLeadJpaEntity toJpaEntity(ContactLead lead) {
        if (lead == null) return null;
        return ContactLeadJpaEntity.builder()
                .id(lead.getId())
                .name(lead.getName())
                .email(lead.getEmail())
                .company(lead.getCompany())
                .message(lead.getMessage())
                .clientIp(lead.getClientIp())
                .createdAt(lead.getCreatedAt())
                .build();
    }
}
