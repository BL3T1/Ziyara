package com.ziyara.backend.domain.entity;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceEarningData(UUID serviceId, String serviceName, int bookingCount, BigDecimal grossRevenue) {}
