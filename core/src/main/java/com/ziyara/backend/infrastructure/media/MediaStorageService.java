package com.ziyara.backend.infrastructure.media;

import java.util.UUID;

/**
 * Port: media file storage.
 * Implementations: {@link LocalMediaStorageService} (dev) and {@link S3MediaStorageService} (prod).
 * Activate via APP_MEDIA_STORAGE_BACKEND=local (default) or APP_MEDIA_STORAGE_BACKEND=s3.
 */
public interface MediaStorageService {

    /**
     * Store a service image and return its public-accessible URL.
     *
     * @param serviceId       owning service's UUID (used for path scoping)
     * @param data            raw image bytes
     * @param contentType     MIME type (e.g. image/jpeg)
     * @param originalFilename client-supplied filename (validated; never used for path construction)
     * @return absolute or context-relative URL suitable for storing in hotel_service_images.url
     */
    String storeServiceImage(UUID serviceId, byte[] data, String contentType, String originalFilename);
}
