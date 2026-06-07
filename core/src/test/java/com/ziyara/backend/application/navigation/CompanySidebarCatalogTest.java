package com.ziyara.backend.application.navigation;

import com.ziyara.backend.domain.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompanySidebarCatalogTest {

    @Test
    void defaultVisible_superAdminIncludesRolesAndApi() {
        List<String> ids = CompanySidebarCatalog.defaultVisibleItemIdsForUserRole(UserRole.SUPER_ADMIN);
        assertTrue(ids.contains("roles"));
        assertTrue(ids.contains("api"));
        assertTrue(ids.contains("integrations"));
        assertTrue(ids.contains("resorts"));
        assertFalse(ids.contains("sales_dashboard"));
        assertFalse(ids.contains("tickets"));
        assertFalse(ids.contains("complaints"));
        assertFalse(ids.contains("reviews"));
    }

    @Test
    void defaultVisible_staffHasDashboardAndManagement() {
        List<String> ids = CompanySidebarCatalog.defaultVisibleItemIdsForUserRole(UserRole.STAFF);
        assertTrue(ids.contains("dashboard"));
        assertTrue(ids.contains("providers"));
    }

    @Test
    void defaultVisible_customerExcludesManagement() {
        List<String> ids = CompanySidebarCatalog.defaultVisibleItemIdsForUserRole(UserRole.CUSTOMER);
        assertTrue(ids.contains("dashboard"));
        assertFalse(ids.contains("providers"));
    }

    @Test
    void sanitize_dropsUnknownAndDupes() {
        List<String> out = CompanySidebarCatalog.sanitizeVisibleItemIds(
                List.of("dashboard", "bogus", "dashboard", "hotels"));
        assertEquals(List.of("dashboard", "hotels"), out);
    }

    @Test
    void assertAllRequestedIdsKnown_rejectsUnknown() {
        assertThrows(IllegalArgumentException.class,
                () -> CompanySidebarCatalog.assertAllRequestedIdsKnown(List.of("not_an_id")));
    }
}
