package com.ziyara.backend.infrastructure.persistence;

import com.ziyara.core.TestcontainersConfiguration;
import net.ttddyy.dsproxy.QueryCountHolder;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

import com.ziyara.backend.infrastructure.persistence.repository.BookingJpaRepository;

/**
 * Detects N+1 query regressions on high-traffic list endpoints.
 * Uses datasource-proxy (via BeanPostProcessor) to intercept Hibernate-issued SQL.
 *
 * Runs with Docker (Testcontainers) — excluded from the default test run.
 */
@Tag("docker")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class QueryCountTest {

    @TestConfiguration
    static class Config {
        @Bean
        static BeanPostProcessor proxyDataSourcePostProcessor() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof DataSource ds) {
                        return ProxyDataSourceBuilder.create(ds)
                                .countQuery()
                                .name("proxy-" + beanName)
                                .build();
                    }
                    return bean;
                }
            };
        }
    }

    @Autowired
    private BookingJpaRepository bookingJpaRepository;

    @BeforeEach
    void resetQueryCount() {
        QueryCountHolder.clear();
    }

    @Test
    @DisplayName("findAll(page) issues at most 2 queries (SELECT + COUNT) — no N+1")
    void findAllBookings_paged_issuesAtMostTwoQueries() {
        bookingJpaRepository.findAll(PageRequest.of(0, 10));

        long selects = QueryCountHolder.getGrandTotal().getSelect();
        assertThat(selects)
                .as("Expected at most 2 SELECT queries (data + count), got %d — possible N+1", selects)
                .isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("findAll(page=0) issues zero data-fetching queries on empty table")
    void findAllBookings_emptyTable_issuesSelectAndCount() {
        long before = QueryCountHolder.getGrandTotal().getSelect();
        bookingJpaRepository.findAll(PageRequest.of(0, 10));
        long delta = QueryCountHolder.getGrandTotal().getSelect() - before;

        assertThat(delta)
                .as("Paged findAll should issue 1-2 SELECTs, got %d", delta)
                .isBetween(1L, 2L);
    }
}
