# RBAC permission editor — manual QA checklist

Run after applying migration `022_rbac_permission_catalogue.sql` (or fresh seed).

1. **Catalogue size** — As Super Admin, open Admin → Roles. Footer shows many permissions (not only three). `GET /roles/permissions/catalogue` returns full list including `system:super_ops` and `system:bulk_export` with `locked: true`.

2. **System role** — Edit permissions on a system role (e.g. Super Admin). All permissions are toggleable, including locked. Save succeeds; reload shows same selection.

3. **Custom role** — Create or edit a custom role. Locked permissions appear disabled with “System only” (EN) / “للنظام فقط” (AR). Saving does not send locked ids; toggling only unlocked works.

4. **API security** — Call `PUT /roles/{id}/permissions` with a non–Super Admin JWT → 403.

5. **API validation** — Custom role + body containing a locked permission UUID → 400. Unknown permission UUID → 400.

6. **Navigation** — Non–Super Admin roles (e.g. HR) no longer see “Roles” in the sidebar; Super Admin still does.

7. **Regression** — Existing users and roles without edits behave as before; Super Admin seed role still has the three original permissions until changed in UI.

**Note:** HTTP enforcement remains `UserRole` / `@PreAuthorize`; DB permission rows are the catalogue for admin UI and future runtime checks.
