package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.PiiFieldRegistryResponse;
import com.ziyara.backend.domain.entity.PiiFieldRegistry;
import com.ziyara.backend.domain.repository.PiiFieldRegistryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PiiRegistryServiceTest {

    @Mock PiiFieldRegistryRepository piiFieldRegistryRepository;

    PiiRegistryService service;

    @BeforeEach
    void setUp() {
        service = new PiiRegistryService(piiFieldRegistryRepository);
    }

    @Test
    void listAll_mapsAllEntries() {
        PiiFieldRegistry entry = new PiiFieldRegistry();
        entry.setId(UUID.randomUUID());
        entry.setTableName("sys_users");
        entry.setColumnName("email");
        entry.setPiiCategory("CONTACT");
        entry.setEncryptionRequired(true);
        entry.setGdprArticle("Art. 6(1)(a)");
        when(piiFieldRegistryRepository.findAll()).thenReturn(List.of(entry));

        List<PiiFieldRegistryResponse> result = service.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTableName()).isEqualTo("sys_users");
        assertThat(result.get(0).getColumnName()).isEqualTo("email");
        assertThat(result.get(0).getPiiCategory()).isEqualTo("CONTACT");
        assertThat(result.get(0).isEncryptionRequired()).isTrue();
        assertThat(result.get(0).getGdprArticle()).isEqualTo("Art. 6(1)(a)");
    }

    @Test
    void listAll_emptyRepo_returnsEmpty() {
        when(piiFieldRegistryRepository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
