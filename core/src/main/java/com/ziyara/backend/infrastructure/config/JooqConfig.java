package com.ziyara.backend.infrastructure.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * jOOQ configuration for CQRS query side.
 * Provides DSLContext for type-safe SQL in query handlers.
 */
@Configuration
public class JooqConfig {

    @Bean
    public DSLContext dslContext(DataSource dataSource) {
        Settings settings = new Settings()
                .withRenderSchema(false)  // use default search_path (public)
                .withRenderQuotedNames(RenderQuotedNames.NEVER);  // unquoted identifiers for PostgreSQL
        return DSL.using(dataSource, SQLDialect.POSTGRES, settings);
    }
}
