package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.PiiFieldRegistryResponse;
import com.ziyara.backend.domain.repository.PiiFieldRegistryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PiiRegistryService {

    private final PiiFieldRegistryRepository piiFieldRegistryRepository;

    public List<PiiFieldRegistryResponse> listAll() {
        return piiFieldRegistryRepository.findAll().stream()
                .map(e -> PiiFieldRegistryResponse.builder()
                        .id(e.getId())
                        .tableName(e.getTableName())
                        .columnName(e.getColumnName())
                        .piiCategory(e.getPiiCategory())
                        .encryptionRequired(e.getEncryptionRequired())
                        .gdprArticle(e.getGdprArticle())
                        .lastReviewedAt(e.getLastReviewedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
