package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.PricePreviewRequest;
import com.ziyara.backend.application.dto.response.PriceBreakdownResponse;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.Service;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.repository.DiscountCodeRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock ServiceRepository serviceRepository;
    @Mock ServiceProviderRepository serviceProviderRepository;
    @Mock DiscountCodeRepository discountCodeRepository;
    @Mock CurrencyService currencyService;
    @Mock DiscountScopeService discountScopeService;

    PricingService pricingService;

    UUID serviceId = UUID.randomUUID();
    UUID providerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        pricingService = new PricingService(
                serviceRepository, serviceProviderRepository,
                discountCodeRepository, currencyService, discountScopeService);
    }

    // ── guard clauses ─────────────────────────────────────────────────────────

    @Test
    void calculatePrice_serviceNotFound_throwsResourceNotFound() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        PricePreviewRequest req = request(serviceId, ServiceType.HOTEL,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(4), 1, 1);

        assertThatThrownBy(() -> pricingService.calculatePrice(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found");
    }

    @Test
    void calculatePrice_providerNotFound_throwsResourceNotFound() {
        Service svc = service(serviceId, providerId, ServiceType.HOTEL, BigDecimal.valueOf(100), "USD");
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(svc));
        when(serviceProviderRepository.findById(providerId)).thenReturn(Optional.empty());

        PricePreviewRequest req = request(serviceId, ServiceType.HOTEL,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(4), 1, 1);

        assertThatThrownBy(() -> pricingService.calculatePrice(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Provider not found");
    }

    // ── HOTEL: base = price × nights × rooms ──────────────────────────────────

    @Test
    void calculatePrice_hotel_3nights2rooms_computesBaseCorrectly() {
        Service svc = service(serviceId, providerId, ServiceType.HOTEL, BigDecimal.valueOf(100), "USD");
        ServiceProvider provider = provider(null); // default 10% commission
        stub(svc, provider);

        // 3 nights, 2 rooms → base = 100 × 3 × 2 = 600
        PricePreviewRequest req = request(serviceId, ServiceType.HOTEL,
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 4), 1, 2);

        PriceBreakdownResponse result = pricingService.calculatePrice(req);

        assertThat(result.getBaseAmount()).isEqualByComparingTo("600.00");
        assertThat(result.getNights()).isEqualTo(3);
        assertThat(result.getCurrency()).isEqualTo("USD");
    }

    @Test
    void calculatePrice_hotel_defaultCommission10Pct_appliedToAfterDiscounts() {
        Service svc = service(serviceId, providerId, ServiceType.HOTEL, BigDecimal.valueOf(100), "USD");
        ServiceProvider provider = provider(null); // null → DEFAULT_COMMISSION_RATE = 10
        stub(svc, provider);

        // 1 night, 1 room → base = 100; total = 100 * 1.10 = 110
        PricePreviewRequest req = request(serviceId, ServiceType.HOTEL,
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 2), 1, 1);

        PriceBreakdownResponse result = pricingService.calculatePrice(req);

        assertThat(result.getBaseAmount()).isEqualByComparingTo("100.00");
        assertThat(result.getCommissionRate()).isEqualByComparingTo("10");
        assertThat(result.getCommissionAmount()).isEqualByComparingTo("10.00");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("110.00");
    }

    @Test
    void calculatePrice_providerOverrideCommission20Pct() {
        Service svc = service(serviceId, providerId, ServiceType.HOTEL, BigDecimal.valueOf(200), "USD");
        ServiceProvider provider = provider(new BigDecimal("20")); // 20% override
        stub(svc, provider);

        // 1 night, 1 room → base = 200; commission = 40; total = 240
        PricePreviewRequest req = request(serviceId, ServiceType.HOTEL,
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 2), 1, 1);

        PriceBreakdownResponse result = pricingService.calculatePrice(req);

        assertThat(result.getCommissionRate()).isEqualByComparingTo("20");
        assertThat(result.getCommissionAmount()).isEqualByComparingTo("40.00");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("240.00");
    }

    // ── TRIP: base = price × guests ───────────────────────────────────────────

    @Test
    void calculatePrice_trip_baseIsPerGuest() {
        Service svc = service(serviceId, providerId, ServiceType.TRIP, BigDecimal.valueOf(50), "USD");
        ServiceProvider provider = provider(null);
        stub(svc, provider);

        // 4 guests → base = 50 × 4 = 200
        PricePreviewRequest req = request(serviceId, ServiceType.TRIP,
                LocalDate.of(2027, 3, 1), null, 4, 1);

        PriceBreakdownResponse result = pricingService.calculatePrice(req);

        assertThat(result.getBaseAmount()).isEqualByComparingTo("200.00");
        assertThat(result.getNights()).isEqualTo(1); // non-hotel → 1
    }

    // ── RESTAURANT: flat fee ──────────────────────────────────────────────────

    @Test
    void calculatePrice_restaurant_flatFee() {
        Service svc = service(serviceId, providerId, ServiceType.RESTAURANT, BigDecimal.valueOf(75), "USD");
        ServiceProvider provider = provider(null);
        stub(svc, provider);

        // flat reservation fee = 75 (not multiplied by guests or rooms)
        PricePreviewRequest req = request(serviceId, ServiceType.RESTAURANT,
                LocalDate.of(2027, 4, 1), null, 4, 1);

        PriceBreakdownResponse result = pricingService.calculatePrice(req);

        assertThat(result.getBaseAmount()).isEqualByComparingTo("75.00");
        assertThat(result.getPricingModel()).contains("Reservation");
    }

    // ── nights computation edge cases ─────────────────────────────────────────

    @Test
    void calculatePrice_hotel_nullCheckOut_defaults1Night() {
        Service svc = service(serviceId, providerId, ServiceType.HOTEL, BigDecimal.valueOf(100), "USD");
        ServiceProvider provider = provider(null);
        stub(svc, provider);

        PricePreviewRequest req = request(serviceId, ServiceType.HOTEL,
                LocalDate.of(2027, 5, 1), null, 1, 1);

        PriceBreakdownResponse result = pricingService.calculatePrice(req);

        assertThat(result.getNights()).isEqualTo(1);
        assertThat(result.getBaseAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void calculatePrice_hotel_checkOutNotAfterCheckIn_defaults1Night() {
        Service svc = service(serviceId, providerId, ServiceType.HOTEL, BigDecimal.valueOf(100), "USD");
        ServiceProvider provider = provider(null);
        stub(svc, provider);

        LocalDate sameDate = LocalDate.of(2027, 5, 10);
        PricePreviewRequest req = request(serviceId, ServiceType.HOTEL, sameDate, sameDate, 1, 1);

        PriceBreakdownResponse result = pricingService.calculatePrice(req);

        assertThat(result.getNights()).isEqualTo(1);
    }

    // ── tax ───────────────────────────────────────────────────────────────────

    @Test
    void calculatePrice_withTaxRate_addedToTotal() {
        Service svc = service(serviceId, providerId, ServiceType.RESTAURANT, BigDecimal.valueOf(100), "USD");
        svc.setTaxRate(new BigDecimal("0.10")); // 10% tax
        ServiceProvider provider = provider(null);
        stub(svc, provider);

        PricePreviewRequest req = request(serviceId, ServiceType.RESTAURANT,
                LocalDate.of(2027, 6, 1), null, 1, 1);

        PriceBreakdownResponse result = pricingService.calculatePrice(req);

        // base 100 × (1 + 10/100) = 110 → taxAmount = 110 × 0.10 = 11 → total = 121
        assertThat(result.getTaxAmount()).isEqualByComparingTo("11.00");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("121.00");
    }

    // ── no discount code → zero discounts ────────────────────────────────────

    @Test
    void calculatePrice_noDiscountCode_zeroDiscounts() {
        Service svc = service(serviceId, providerId, ServiceType.HOTEL, BigDecimal.valueOf(100), "USD");
        ServiceProvider provider = provider(null);
        stub(svc, provider);

        PricePreviewRequest req = PricePreviewRequest.builder()
                .serviceId(serviceId)
                .checkInDate(LocalDate.of(2027, 7, 1))
                .checkOutDate(LocalDate.of(2027, 7, 2))
                .build();

        PriceBreakdownResponse result = pricingService.calculatePrice(req);

        assertThat(result.getProviderDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getCompanyDiscountAmount()).isEqualByComparingTo("0.00");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Service service(UUID id, UUID provId, ServiceType type, BigDecimal basePrice, String currency) {
        Service s = new Service();
        s.setId(id);
        s.setProviderId(provId);
        s.setType(type);
        s.setBasePrice(basePrice);
        s.setCurrency(currency);
        return s;
    }

    private ServiceProvider provider(BigDecimal commissionRate) {
        ServiceProvider p = new ServiceProvider();
        p.setId(providerId);
        p.setCommissionRate(commissionRate);
        return p;
    }

    private void stub(Service svc, ServiceProvider provider) {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(svc));
        when(serviceProviderRepository.findById(providerId)).thenReturn(Optional.of(provider));
    }

    private PricePreviewRequest request(UUID svcId, ServiceType type,
                                         LocalDate checkIn, LocalDate checkOut,
                                         int guests, int rooms) {
        return PricePreviewRequest.builder()
                .serviceId(svcId)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .guests(guests)
                .rooms(rooms)
                .build();
    }
}
