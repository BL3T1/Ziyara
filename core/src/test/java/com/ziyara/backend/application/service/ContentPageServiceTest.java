package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.UpsertContentPageRequest;
import com.ziyara.backend.application.dto.response.ContentPageResponse;
import com.ziyara.backend.domain.entity.ContentPage;
import com.ziyara.backend.domain.repository.ContentPageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentPageServiceTest {

    @Mock ContentPageRepository contentPageRepository;

    ContentPageService service;

    @BeforeEach
    void setUp() {
        service = new ContentPageService(contentPageRepository);
    }

    // ── getPublicPage ─────────────────────────────────────────────────────────

    @Test
    void getPublicPage_publishedPageExists_returnsIt() {
        ContentPage page = new ContentPage();
        page.setSlug("about");
        page.setPublished(true);
        page.setContentEn(Map.of("title", "About Us"));

        when(contentPageRepository.findBySlug("about")).thenReturn(Optional.of(page));

        ContentPageResponse result = service.getPublicPage("about", "en");

        assertThat(result.getSlug()).isEqualTo("about");
        assertThat(result.getContent()).containsKey("title");
    }

    @Test
    void getPublicPage_unpublishedPage_returnsFallback() {
        ContentPage page = new ContentPage();
        page.setSlug("draft");
        page.setPublished(false);

        when(contentPageRepository.findBySlug("draft")).thenReturn(Optional.of(page));

        ContentPageResponse result = service.getPublicPage("draft", "en");

        assertThat(result.getSlug()).isEqualTo("draft");
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getPublicPage_noPage_returnsFallbackWithEmptyContent() {
        when(contentPageRepository.findBySlug("missing")).thenReturn(Optional.empty());

        ContentPageResponse result = service.getPublicPage("missing", null);

        assertThat(result.getSlug()).isEqualTo("missing");
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getPublicPage_slugNormalized_lowercaseAndTrimmed() {
        when(contentPageRepository.findBySlug("privacy")).thenReturn(Optional.empty());

        service.getPublicPage("  Privacy  ", "en");

        verify(contentPageRepository).findBySlug("privacy");
    }

    @Test
    void getPublicPage_arabicLang_returnsArabicContent() {
        ContentPage page = new ContentPage();
        page.setSlug("about");
        page.setPublished(true);
        page.setContentEn(Map.of("title", "About"));
        page.setContentAr(Map.of("title", "حول"));

        when(contentPageRepository.findBySlug("about")).thenReturn(Optional.of(page));

        ContentPageResponse result = service.getPublicPage("about", "ar");

        assertThat(result.getContent()).containsEntry("title", "حول");
    }

    // ── upsert ────────────────────────────────────────────────────────────────

    @Test
    void upsert_newPage_createsAndSaves() {
        when(contentPageRepository.findBySlug("terms")).thenReturn(Optional.empty());

        ContentPage saved = new ContentPage();
        saved.setSlug("terms");
        saved.setPublished(true);
        saved.setContentEn(Map.of("body", "Terms text"));
        when(contentPageRepository.save(any())).thenReturn(saved);

        UpsertContentPageRequest request = new UpsertContentPageRequest();
        request.setContentEn(Map.of("body", "Terms text"));

        ContentPageResponse result = service.upsert("terms", request);

        assertThat(result.getSlug()).isEqualTo("terms");
        verify(contentPageRepository).save(any());
    }

    @Test
    void upsert_existingPage_updatesContent() {
        ContentPage existing = new ContentPage();
        existing.setSlug("about");
        existing.setPublished(true);
        existing.setContentEn(Map.of("old", "content"));
        when(contentPageRepository.findBySlug("about")).thenReturn(Optional.of(existing));

        ContentPage saved = new ContentPage();
        saved.setSlug("about");
        saved.setPublished(true);
        saved.setContentEn(Map.of("new", "content"));
        when(contentPageRepository.save(any())).thenReturn(saved);

        UpsertContentPageRequest request = new UpsertContentPageRequest();
        request.setContentEn(Map.of("new", "content"));
        request.setPublished(true);

        ContentPageResponse result = service.upsert("about", request);

        assertThat(result.getSlug()).isEqualTo("about");
        verify(contentPageRepository).save(existing);
    }

    @Test
    void upsert_publishedNullDefaults_toTrue() {
        when(contentPageRepository.findBySlug("faq")).thenReturn(Optional.empty());

        ContentPage saved = new ContentPage();
        saved.setSlug("faq");
        saved.setPublished(true);
        saved.setContentEn(Map.of());
        when(contentPageRepository.save(any())).thenReturn(saved);

        UpsertContentPageRequest request = new UpsertContentPageRequest();

        service.upsert("faq", request);

        ArgumentCaptor<ContentPage> captor = ArgumentCaptor.forClass(ContentPage.class);
        verify(contentPageRepository).save(captor.capture());
        assertThat(captor.getValue().getPublished()).isTrue();
    }
}
