package com.ziyara.backend.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Media storage configuration. Supports local filesystem (default) and AWS S3.
 * Switch via APP_MEDIA_STORAGE_BACKEND=local|s3.
 */
@Data
@ConfigurationProperties(prefix = "app.media")
public class MediaStorageProperties {

    /** local or s3 — selects which MediaStorageService bean is active. */
    private String storageBackend = "local";

    /** Root directory for local storage; uploaded files live under {@code services/{serviceId}/}. */
    private String storageRoot = "./data/media";

    /**
     * Optional absolute public base for stored URLs (e.g. {@code https://cdn.example.com/api/v1}).
     * For S3 with a CloudFront CDN, set this to the CloudFront domain.
     * If empty: local uses context-path, S3 uses bucket virtual-hosted URL.
     */
    private String publicBaseUrl = "";

    // S3-specific configuration (used when storage-backend=s3)
    private String s3Bucket = "";
    private String s3Region = "us-east-1";
}
