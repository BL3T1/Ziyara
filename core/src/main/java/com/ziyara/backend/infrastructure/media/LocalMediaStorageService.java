package com.ziyara.backend.infrastructure.media;

import com.ziyara.backend.infrastructure.config.MediaStorageProperties;
import com.ziyara.backend.application.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Persists uploaded bytes under {@link MediaStorageProperties#getStorageRoot()} and returns a public URL path.
 * Active when APP_MEDIA_STORAGE_BACKEND=local (default).
 */
@Service
@ConditionalOnProperty(name = "app.media.storage-backend", havingValue = "local", matchIfMissing = true)
public class LocalMediaStorageService implements MediaStorageService {

    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private final MediaStorageProperties properties;
    private final String servletContextPath;

    public LocalMediaStorageService(
            MediaStorageProperties properties,
            @Value("${server.servlet.context-path:}") String servletContextPath) {
        this.properties = properties;
        this.servletContextPath = servletContextPath == null ? "" : servletContextPath;
    }

    /**
     * @return URL suitable for storing on {@code hotel_service_images.url} (browser-fetchable).
     */
    public String storeServiceImage(UUID serviceId, byte[] data, String contentType, String originalFilename) {
        if (data == null || data.length == 0) {
            throw new BusinessException("Empty file");
        }
        if (data.length > MAX_BYTES) {
            throw new BusinessException("File too large (max 10MB)");
        }
        validateOriginalFilename(originalFilename);
        String normalizedType = normalizeContentType(contentType);
        if (!ALLOWED_TYPES.contains(normalizedType)) {
            throw new BusinessException("Unsupported image type (use JPEG, PNG, WebP, or GIF)");
        }
        // Never use client-provided filenames for path construction. We only use the validated content-type
        // to choose a safe extension and generate a server-side UUID filename.
        String ext = extensionFor(normalizedType);
        String fileName = UUID.randomUUID() + ext;

        Path base = Paths.get(properties.getStorageRoot()).toAbsolutePath().normalize();
        // Defensive: ensure our computed directory/target stay under the configured base.
        Path dir = resolveUnderBase(base, base.resolve("services").resolve(serviceId.toString()));
        try {
            Files.createDirectories(dir);
            if (fileName.contains("/") || fileName.contains("\\") || fileName.contains(":")) {
                throw new BusinessException("Invalid media filename");
            }
            Path target = resolveUnderBase(base, dir.resolve(fileName));
            Files.write(target, data);
        } catch (IOException e) {
            throw new BusinessException("Failed to store file: " + e.getMessage());
        }

        String relative = "services/" + serviceId + "/" + fileName;
        return buildPublicUrl(relative);
    }

    @Override
    public String storeProviderImage(UUID providerId, byte[] data, String contentType, String originalFilename) {
        if (data == null || data.length == 0) throw new BusinessException("Empty file");
        if (data.length > MAX_BYTES) throw new BusinessException("File too large (max 10MB)");
        validateOriginalFilename(originalFilename);
        String normalizedType = normalizeContentType(contentType);
        if (!ALLOWED_TYPES.contains(normalizedType)) throw new BusinessException("Unsupported image type (use JPEG, PNG, WebP, or GIF)");
        String ext = extensionFor(normalizedType);
        String fileName = UUID.randomUUID() + ext;
        Path base = Paths.get(properties.getStorageRoot()).toAbsolutePath().normalize();
        Path dir = resolveUnderBase(base, base.resolve("providers").resolve(providerId.toString()));
        try {
            Files.createDirectories(dir);
            if (fileName.contains("/") || fileName.contains("\\") || fileName.contains(":")) throw new BusinessException("Invalid media filename");
            Path target = resolveUnderBase(base, dir.resolve(fileName));
            Files.write(target, data);
        } catch (IOException e) {
            throw new BusinessException("Failed to store file: " + e.getMessage());
        }
        String relative = "providers/" + providerId + "/" + fileName;
        return buildPublicUrl(relative);
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        int semi = contentType.indexOf(';');
        return (semi > 0 ? contentType.substring(0, semi) : contentType).trim().toLowerCase(Locale.ROOT);
    }

    private String buildPublicUrl(String relativeUnderMedia) {
        String configured = properties.getPublicBaseUrl();
        if (configured != null && !configured.isBlank()) {
            String c = configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
            return c + "/media/" + relativeUnderMedia;
        }
        String cp = servletContextPath.endsWith("/") && servletContextPath.length() > 1
                ? servletContextPath.substring(0, servletContextPath.length() - 1)
                : servletContextPath;
        return cp + "/media/" + relativeUnderMedia;
    }

    private static Path resolveUnderBase(Path base, Path candidate) {
        Path baseNorm = base.toAbsolutePath().normalize();
        Path candNorm = candidate.toAbsolutePath().normalize();
        if (!candNorm.startsWith(baseNorm)) {
            throw new BusinessException("Invalid media storage target");
        }
        return candNorm;
    }

    private static void validateOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return;
        }
        // Attackers can send crafted "filenames" that look like paths. We explicitly reject these
        // inputs even though we only persist a server-generated UUID filename.
        String s = originalFilename.trim();
        String lower = s.toLowerCase(Locale.ROOT);

        // Absolute paths / separators.
        if (s.contains("/") || s.contains("\\") || s.contains(":")) {
            throw new BusinessException("Invalid filename");
        }
        // Path traversal patterns.
        if (s.contains("..")) {
            throw new BusinessException("Invalid filename");
        }

        // Encoded traversal hints (common percent-encoded sequences).
        // Note: multipart clients may URL-encode the header value.
        if (lower.contains("%2e") || lower.contains("%2f") || lower.contains("%5c")) {
            throw new BusinessException("Invalid filename");
        }
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            // Should never happen because we validate against ALLOWED_TYPES above.
            default -> throw new BusinessException("Unsupported image type (internal mismatch)");
        };
    }
}
