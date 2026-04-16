package com.ziyara.backend.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Local filesystem storage for uploaded service images (MVP B).
 */
@Data
@ConfigurationProperties(prefix = "app.media")
public class MediaStorageProperties {

    /**
     * Root directory; uploaded files live under {@code services/{serviceId}/}.
     */
    private String storageRoot = "./data/media";

    /**
     * Optional absolute public base for stored URLs (e.g. {@code https://cdn.example.com/api/v1}).
     * If empty, URLs are relative: {@code {server.servlet.context-path}/media/...}.
     */
    private String publicBaseUrl = "";
}
