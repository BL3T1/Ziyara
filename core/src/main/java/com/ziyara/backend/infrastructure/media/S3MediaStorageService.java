package com.ziyara.backend.infrastructure.media;

import com.ziyara.backend.infrastructure.config.MediaStorageProperties;
import com.ziyara.backend.application.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * AWS S3 media storage. Activated when APP_MEDIA_STORAGE_BACKEND=s3.
 * Credentials are sourced from the default AWS credential chain
 * (env vars AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY, or IAM instance role in ECS/EC2).
 */
@Service
@ConditionalOnProperty(name = "app.media.storage-backend", havingValue = "s3")
@Slf4j
public class S3MediaStorageService implements MediaStorageService {

    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private final MediaStorageProperties properties;
    private final S3Client s3Client;

    public S3MediaStorageService(MediaStorageProperties properties) {
        this.properties = properties;
        this.s3Client = S3Client.builder()
                .region(Region.of(properties.getS3Region()))
                .build();
    }

    @Override
    public String storeServiceImage(UUID serviceId, byte[] data, String contentType, String originalFilename) {
        if (data == null || data.length == 0) throw new BusinessException("Empty file");
        if (data.length > MAX_BYTES) throw new BusinessException("File too large (max 10MB)");

        String normalizedType = normalizeContentType(contentType);
        if (!ALLOWED_TYPES.contains(normalizedType)) {
            throw new BusinessException("Unsupported image type (use JPEG, PNG, WebP, or GIF)");
        }
        String ext = extensionFor(normalizedType);
        String key = "services/" + serviceId + "/" + UUID.randomUUID() + ext;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getS3Bucket())
                .key(key)
                .contentType(normalizedType)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(data));
        } catch (Exception e) {
            log.error("S3 upload failed for key {}: {}", key, e.getMessage());
            throw new BusinessException("Media upload failed: " + e.getMessage());
        }

        return buildPublicUrl(key);
    }

    private String buildPublicUrl(String key) {
        String base = properties.getPublicBaseUrl();
        if (base != null && !base.isBlank()) {
            String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
            return b + "/" + key;
        }
        // Default: https://{bucket}.s3.{region}.amazonaws.com/{key}
        return "https://" + properties.getS3Bucket() + ".s3." + properties.getS3Region() + ".amazonaws.com/" + key;
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) return "";
        int semi = contentType.indexOf(';');
        return (semi > 0 ? contentType.substring(0, semi) : contentType).trim().toLowerCase(Locale.ROOT);
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> throw new BusinessException("Unsupported image type");
        };
    }
}
