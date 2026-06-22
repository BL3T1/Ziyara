package com.ziyara.backend.infrastructure.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gateway.disbursement")
public class PayoutDisbursementProperties {

    /** When true, approved payouts are automatically dispatched to the provider's bank. */
    private boolean autoDisburse = false;

    /** Disbursement provider: "stub" (default) or a real integration key. */
    private String provider = "stub";

    public boolean isAutoDisburse() { return autoDisburse; }
    public void setAutoDisburse(boolean autoDisburse) { this.autoDisburse = autoDisburse; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
