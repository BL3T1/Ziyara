package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.DataExportRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataExportRequestRepository {

    DataExportRequest save(DataExportRequest request);

    Optional<DataExportRequest> findById(UUID id);

    List<DataExportRequest> findByUserIdOrderedDesc(UUID userId);
}
