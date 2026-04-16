package com.ziyara.backend.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves stored uploads at {@code {contextPath}/media/**} from {@link MediaStorageProperties#getStorageRoot()}.
 */
@Configuration
@RequiredArgsConstructor
public class MediaStaticResourceConfig implements WebMvcConfigurer {

    private final MediaStorageProperties mediaStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String root = mediaStorageProperties.getStorageRoot();
        if (root == null || root.isBlank()) {
            return;
        }
        Path base = Paths.get(root).toAbsolutePath().normalize();
        String location = base.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/media/**").addResourceLocations(location);
    }
}
