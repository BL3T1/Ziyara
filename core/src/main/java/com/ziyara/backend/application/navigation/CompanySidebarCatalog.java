package com.ziyara.backend.application.navigation;

import com.ziyara.backend.domain.enums.UserRole;

import java.util.*;

/**
 * Server-side copy of {@code front/my-app/src/config/sidebar.ts} (section + item IDs + allowedRoles).
 * Keep in sync when dashboard nav changes.
 */
public final class CompanySidebarCatalog {

    private CompanySidebarCatalog() {}

    private static final Map<SidebarSurface, Set<String>> SECTION_IDS_BY_SURFACE = Map.ofEntries(
            Map.entry(SidebarSurface.SUPER_ADMIN, Set.of("main", "services", "management", "admin")),
            Map.entry(SidebarSurface.ADMIN, Set.of("main", "services", "management", "support")),
            Map.entry(SidebarSurface.FINANCE, Set.of("main", "management")),
            Map.entry(SidebarSurface.SUPPORT, Set.of("main", "support")),
            Map.entry(SidebarSurface.EXECUTIVE, Set.of("main", "services", "management", "support")),
            Map.entry(SidebarSurface.PROVIDER, Set.of("main", "services")),
            // USER = B2C consumer (backend CUSTOMER); not company staff directory.
            Map.entry(SidebarSurface.USER, Set.of("main", "services"))
    );

    private record ItemDef(String id, String sectionId, Set<SidebarSurface> allowedSurfacesOrNull) {}

    /** Canonical order matches {@code SIDEBAR_SECTIONS} in sidebar.ts */
    private static final List<ItemDef> ITEMS = List.of(
            new ItemDef("dashboard", "main", null),
            new ItemDef("main_find_customer", "main", EnumSet.of(SidebarSurface.SUPER_ADMIN)),
            new ItemDef("main_deleted_items", "main", EnumSet.of(SidebarSurface.SUPER_ADMIN)),
            new ItemDef("hotels", "services", null),
            new ItemDef("resorts", "services", null),
            new ItemDef("restaurants", "services", null),
            new ItemDef("taxis", "services", null),
            new ItemDef("trips", "services", null),
            new ItemDef("providers", "management", null),
            new ItemDef("bookings", "management", null),
            new ItemDef("payments", "management", null),
            new ItemDef("payouts", "management", EnumSet.of(SidebarSurface.SUPER_ADMIN, SidebarSurface.ADMIN, SidebarSurface.FINANCE, SidebarSurface.EXECUTIVE)),
            new ItemDef("discounts", "management", null),
            new ItemDef("media_approvals", "management", EnumSet.of(SidebarSurface.SUPER_ADMIN, SidebarSurface.ADMIN)),
            new ItemDef("profile_edit_requests", "management", EnumSet.of(SidebarSurface.SUPER_ADMIN, SidebarSurface.ADMIN)),
            new ItemDef("identity_verifications", "management", EnumSet.of(SidebarSurface.SUPER_ADMIN, SidebarSurface.ADMIN)),
            new ItemDef("reports", "management", null),
            new ItemDef("taxi_trips", "management", null),
            new ItemDef("currency_rates", "management", EnumSet.of(SidebarSurface.SUPER_ADMIN, SidebarSurface.EXECUTIVE, SidebarSurface.FINANCE)),
            new ItemDef("complaints", "support", null),
            new ItemDef("reviews", "support", null),
            new ItemDef("provider_messages", "support", null),
            new ItemDef("settings", "admin", null),
            new ItemDef("users", "admin", EnumSet.of(SidebarSurface.SUPER_ADMIN)),
            new ItemDef("roles", "admin", null),
            new ItemDef("logs", "admin", null),
            new ItemDef("content", "admin", EnumSet.of(SidebarSurface.SUPER_ADMIN, SidebarSurface.ADMIN)),
            new ItemDef("api", "admin", EnumSet.of(SidebarSurface.SUPER_ADMIN)),
            new ItemDef("integrations", "admin", EnumSet.of(SidebarSurface.SUPER_ADMIN, SidebarSurface.EXECUTIVE, SidebarSurface.ADMIN)),
            new ItemDef("webhooks", "admin", EnumSet.of(SidebarSurface.SUPER_ADMIN, SidebarSurface.EXECUTIVE, SidebarSurface.ADMIN)),
            new ItemDef("subscriptions", "admin", EnumSet.of(SidebarSurface.SUPER_ADMIN, SidebarSurface.EXECUTIVE, SidebarSurface.ADMIN))
    );

    private static final Set<String> ALLOWED_IDS;

    static {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (ItemDef def : ITEMS) {
            ids.add(def.id);
        }
        ALLOWED_IDS = Collections.unmodifiableSet(ids);
    }

    public static Set<String> allowedItemIds() {
        return ALLOWED_IDS;
    }

    public static List<String> defaultVisibleItemIdsForUserRole(UserRole userRole) {
        return defaultVisibleItemIds(SidebarSurface.fromUserRole(userRole));
    }

    public static List<String> defaultVisibleItemIds(SidebarSurface surface) {
        Set<String> sections = SECTION_IDS_BY_SURFACE.getOrDefault(surface, Set.of());
        List<String> out = new ArrayList<>();
        for (ItemDef def : ITEMS) {
            if (!sections.contains(def.sectionId)) {
                continue;
            }
            if (def.allowedSurfacesOrNull != null && !def.allowedSurfacesOrNull.contains(surface)) {
                continue;
            }
            out.add(def.id);
        }
        return out;
    }

    /**
     * Keeps only known IDs, preserves first occurrence order, drops duplicates.
     */
    public static void assertAllRequestedIdsKnown(List<String> requested) {
        if (requested == null) {
            return;
        }
        for (String id : requested) {
            if (id == null || id.isBlank()) {
                continue;
            }
            String t = id.trim();
            if (!ALLOWED_IDS.contains(t)) {
                throw new IllegalArgumentException("Unknown navigation item id: " + t);
            }
        }
    }

    public static List<String> sanitizeVisibleItemIds(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<String> out = new ArrayList<>();
        for (String id : requested) {
            if (id == null || id.isBlank()) {
                continue;
            }
            String t = id.trim();
            if (!ALLOWED_IDS.contains(t) || !seen.add(t)) {
                continue;
            }
            out.add(t);
        }
        return out;
    }
}
