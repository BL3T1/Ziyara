package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.infrastructure.rls.RlsAwareDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Wraps the primary {@link DataSource} so pooled connections receive per-request RLS GUCs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RlsDataSourceBeanPostProcessor implements BeanPostProcessor {

    @Value("${ziyara.rls.enabled:false}")
    private boolean rlsEnabled;

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        if (!rlsEnabled || !(bean instanceof DataSource)) {
            return bean;
        }
        if (bean instanceof RlsAwareDataSource) {
            return bean;
        }
        if (!"dataSource".equals(beanName)) {
            return bean;
        }
        log.info("Wrapping DataSource bean '{}' with RlsAwareDataSource", beanName);
        return new RlsAwareDataSource((DataSource) bean);
    }
}
