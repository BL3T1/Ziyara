package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.PortalSupportRequest;
import com.ziyara.backend.infrastructure.persistence.entity.PortalSupportRequestJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PortalSupportRequestMapper {

    public PortalSupportRequest toDomainEntity(PortalSupportRequestJpaEntity entity) {
        if (entity == null) return null;
        PortalSupportRequest request = new PortalSupportRequest();
        request.setId(entity.getId());
        request.setProviderId(entity.getProviderId());
        request.setUserId(entity.getUserId());
        request.setSubject(entity.getSubject());
        request.setBody(entity.getBody());
        request.setCreatedAt(entity.getCreatedAt());
        request.setStaffResponse(entity.getStaffResponse());
        request.setRespondedAt(entity.getRespondedAt());
        request.setRespondedByUserId(entity.getRespondedByUserId());
        return request;
    }

    public PortalSupportRequestJpaEntity toJpaEntity(PortalSupportRequest request) {
        if (request == null) return null;
        return PortalSupportRequestJpaEntity.builder()
                .id(request.getId())
                .providerId(request.getProviderId())
                .userId(request.getUserId())
                .subject(request.getSubject())
                .body(request.getBody())
                .createdAt(request.getCreatedAt())
                .staffResponse(request.getStaffResponse())
                .respondedAt(request.getRespondedAt())
                .respondedByUserId(request.getRespondedByUserId())
                .build();
    }
}
