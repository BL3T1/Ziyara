package com.ziyara.backend.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.backend.application.dto.request.UpdateSystemSettingsRequest;
import com.ziyara.backend.application.dto.response.SystemSettingsResponse;
import com.ziyara.backend.domain.entity.SystemSetting;
import com.ziyara.backend.domain.repository.SystemSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static com.ziyara.backend.application.service.SystemSettingsService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SystemSettingsServiceTest {

    @Mock SystemSettingRepository repository;

    SystemSettingsService service;

    @BeforeEach
    void setUp() {
        service = new SystemSettingsService(repository, new ObjectMapper());
    }

    // ── getSettings ───────────────────────────────────────────────────────────

    @Test
    void getSettings_noDbEntries_returnsDefaults() {
        when(repository.findByKey(anyString())).thenReturn(Optional.empty());

        SystemSettingsResponse result = service.getSettings();

        assertThat(result.getCompanyDisplayName()).isEqualTo("Ziyara");
        assertThat(result.getDefaultCurrency()).isEqualTo("USD");
        assertThat(result.isMaintenanceMode()).isFalse();
        assertThat(result.isProviderMaintenanceMode()).isFalse();
    }

    @Test
    void getSettings_withStoredValues_returnsStoredValues() {
        when(repository.findByKey(KEY_COMPANY_DISPLAY_NAME))
                .thenReturn(Optional.of(settingWithJson("{\"v\":\"MyCompany\"}")));
        when(repository.findByKey(KEY_DEFAULT_CURRENCY))
                .thenReturn(Optional.of(settingWithJson("{\"v\":\"EUR\"}")));
        when(repository.findByKey(KEY_MAINTENANCE_MODE))
                .thenReturn(Optional.of(settingWithJson("{\"v\":true}")));
        when(repository.findByKey(KEY_PROVIDER_MAINTENANCE_MODE))
                .thenReturn(Optional.empty());

        SystemSettingsResponse result = service.getSettings();

        assertThat(result.getCompanyDisplayName()).isEqualTo("MyCompany");
        assertThat(result.getDefaultCurrency()).isEqualTo("EUR");
        assertThat(result.isMaintenanceMode()).isTrue();
        assertThat(result.isProviderMaintenanceMode()).isFalse();
    }

    @Test
    void getSettings_malformedJson_returnsDefault() {
        when(repository.findByKey(KEY_COMPANY_DISPLAY_NAME))
                .thenReturn(Optional.of(settingWithJson("not-json")));
        when(repository.findByKey(anyString())).thenReturn(Optional.empty());

        SystemSettingsResponse result = service.getSettings();

        assertThat(result.getCompanyDisplayName()).isEqualTo("Ziyara");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_companyName_savesUpsertedValue() {
        UUID userId = UUID.randomUUID();
        UpdateSystemSettingsRequest request = new UpdateSystemSettingsRequest();
        request.setCompanyDisplayName("NewCo");
        when(repository.findByKey(anyString())).thenReturn(Optional.empty());

        service.update(request, userId);

        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        boolean hasCompanyNameKey = captor.getAllValues().stream()
                .anyMatch(s -> KEY_COMPANY_DISPLAY_NAME.equals(s.getSettingKey()));
        assertThat(hasCompanyNameKey).isTrue();
    }

    @Test
    void update_maintenanceMode_savesValue() {
        UUID userId = UUID.randomUUID();
        UpdateSystemSettingsRequest request = new UpdateSystemSettingsRequest();
        request.setMaintenanceMode(true);
        when(repository.findByKey(anyString())).thenReturn(Optional.empty());

        service.update(request, userId);

        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        boolean hasMaintenanceKey = captor.getAllValues().stream()
                .anyMatch(s -> KEY_MAINTENANCE_MODE.equals(s.getSettingKey()));
        assertThat(hasMaintenanceKey).isTrue();
    }

    @Test
    void update_nullFields_skipsThoseKeys() {
        UUID userId = UUID.randomUUID();
        UpdateSystemSettingsRequest request = new UpdateSystemSettingsRequest();

        service.update(request, userId);

        verify(repository, never()).save(any());
    }

    @Test
    void update_existingKey_updatesExistingSetting() {
        UUID userId = UUID.randomUUID();
        SystemSetting existing = settingWithJson("{\"v\":\"OldCo\"}");
        existing.setSettingKey(KEY_COMPANY_DISPLAY_NAME);
        when(repository.findByKey(KEY_COMPANY_DISPLAY_NAME)).thenReturn(Optional.of(existing));

        UpdateSystemSettingsRequest request = new UpdateSystemSettingsRequest();
        request.setCompanyDisplayName("UpdatedCo");

        service.update(request, userId);

        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSettingKey()).isEqualTo(KEY_COMPANY_DISPLAY_NAME);
    }

    private SystemSetting settingWithJson(String json) {
        SystemSetting s = new SystemSetting();
        s.setValueJson(json);
        return s;
    }
}
