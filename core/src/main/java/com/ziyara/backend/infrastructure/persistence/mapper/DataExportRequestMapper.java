package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.DataExportRequest;
import com.ziyara.backend.infrastructure.persistence.entity.DataExportRequestJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class DataExportRequestMapper {

    public DataExportRequest toDomainEntity(DataExportRequestJpaEntity entity) {
        if (entity == null) return null;
        DataExportRequest request = new DataExportRequest();
        request.setId(entity.getId());
        request.setUserId(entity.getUserId());
        request.setStatus(entity.getStatus());
        request.setFormat(entity.getFormat());
        request.setExportPath(entity.getExportPath());
        request.setRequestedAt(entity.getRequestedAt());
        request.setCompletedAt(entity.getCompletedAt());
        request.setExpiresAt(entity.getExpiresAt());
        request.setFailureReason(entity.getFailureReason());
        request.setPayloadJson(entity.getPayloadJson());
        request.setRecordCount(entity.getRecordCount());
        return request;
    }

    public DataExportRequestJpaEntity toJpaEntity(DataExportRequest request) {
        if (request == null) return null;
        return DataExportRequestJpaEntity.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .status(request.getStatus())
                .format(request.getFormat())
                .exportPath(request.getExportPath())
                .requestedAt(request.getRequestedAt())
                .completedAt(request.getCompletedAt())
                .expiresAt(request.getExpiresAt())
                .failureReason(request.getFailureReason())
                .payloadJson(request.getPayloadJson())
                .recordCount(request.getRecordCount())
                .build();
    }
}
