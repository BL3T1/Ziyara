package com.ziyara.backend.infrastructure.media;

import com.ziyara.backend.infrastructure.config.MediaStorageProperties;
import com.ziyara.backend.presentation.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LocalMediaStorageServiceTest {

    @Test
    void storeServiceImage_ShouldWriteFileAndReturnContextPathUrl(@TempDir Path tempDir) throws Exception {
        MediaStorageProperties props = new MediaStorageProperties();
        props.setStorageRoot(tempDir.toString());
        LocalMediaStorageService svc = new LocalMediaStorageService(props, "/api/v1");

        UUID serviceId = UUID.randomUUID();
        String url = svc.storeServiceImage(serviceId, new byte[] {1, 2, 3}, "image/png", "x.png");

        assertTrue(url.startsWith("/api/v1/media/services/" + serviceId + "/"));
        assertTrue(url.endsWith(".png"));
        Path dir = tempDir.resolve("services").resolve(serviceId.toString());
        assertTrue(Files.isDirectory(dir));
        assertEquals(1, Files.list(dir).count());
    }

    @Test
    void storeServiceImage_WhenDisallowedType_ShouldThrow(@TempDir Path tempDir) {
        MediaStorageProperties props = new MediaStorageProperties();
        props.setStorageRoot(tempDir.toString());
        LocalMediaStorageService svc = new LocalMediaStorageService(props, "");

        assertThrows(
                BusinessException.class,
                () -> svc.storeServiceImage(UUID.randomUUID(), new byte[] {1}, "application/pdf", "a.pdf"));
    }

    @Test
    void storeServiceImage_WhenOriginalFilenameLooksLikeTraversal_ShouldThrow(@TempDir Path tempDir) {
        MediaStorageProperties props = new MediaStorageProperties();
        props.setStorageRoot(tempDir.toString());
        LocalMediaStorageService svc = new LocalMediaStorageService(props, "/api/v1");

        UUID serviceId = UUID.randomUUID();
        List<String> invalidOriginalFilenames = List.of(
                "../..//evil.png",
                "..\\..\\evil.png",
                "/etc/passwd",
                "C:\\evil.png",
                "%2e%2e%2f..%2f..%2fsecret.png",
                "evil..png",
                "evil%2f..%2fsecret.png"
        );

        for (String filename : invalidOriginalFilenames) {
            assertThrows(
                    BusinessException.class,
                    () -> svc.storeServiceImage(serviceId, new byte[] {1, 2, 3}, "image/png", filename),
                    "Expected invalid filename to be rejected: " + filename
            );
        }
    }
}
