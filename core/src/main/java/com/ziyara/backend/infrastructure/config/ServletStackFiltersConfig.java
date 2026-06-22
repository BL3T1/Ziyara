package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.infrastructure.web.CorrelationIdFilter;
import com.ziyara.backend.infrastructure.web.LocaleFilter;
import com.ziyara.backend.infrastructure.web.SecurityHeadersFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

/**
 * Registers early servlet filters outside Spring Security's filter chain ordering constraints.
 */
@Configuration
@Profile("!functest")
public class ServletStackFiltersConfig {

    @Bean
    public SecurityHeadersFilter securityHeadersFilter(
            @Value("${ziyara.security.headers.hsts-enabled:false}") boolean hstsEnabled,
            @Value("${ziyara.security.headers.https-enforce:false}") boolean httpsEnforce) {
        return new SecurityHeadersFilter(hstsEnabled, httpsEnforce);
    }

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    public LocaleFilter localeFilter() {
        return new LocaleFilter();
    }

    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilterRegistration(SecurityHeadersFilter filter) {
        FilterRegistrationBean<SecurityHeadersFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 4);
        bean.addUrlPatterns("/*");
        return bean;
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter filter) {
        FilterRegistrationBean<CorrelationIdFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        bean.addUrlPatterns("/*");
        return bean;
    }

    @Bean
    public FilterRegistrationBean<LocaleFilter> localeFilterRegistration(LocaleFilter filter) {
        FilterRegistrationBean<LocaleFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 6);
        bean.addUrlPatterns("/*");
        return bean;
    }
}
