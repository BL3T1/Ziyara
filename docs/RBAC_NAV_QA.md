# RBAC sidebar navigation — manual QA checklist

Use after deploying migration `V4__roles_navigation_item_ids` and the RBAC APIs.

1. **Custom role, subset of items**  
   Create a custom role, set sidebar items via Admin → Roles → Sidebar, assign a staff user via Management → Groups (RBAC). That user should see only the selected links; RTL layout should remain correct.

2. **No RBAC assignment**  
   Staff user with no `sys_user_roles` row should see the same sidebar as before (from `UserRole` + static `ROLE_SIDEBAR_SECTIONS` rules).

3. **Direct URL**  
   Opening a route hidden from the sidebar should not crash the app; APIs may return 403 per existing `@PreAuthorize`.

4. **Navigation after login**  
   After login, company staff sidebar should match `GET /users/me/navigation` for `source: rbac_role`; refresh or new tab should refetch via layout bootstrap.

5. **Invalid item id**  
   `PUT /roles/{id}/navigation` with an unknown item id should return 400.

6. **Super Admin — system role**  
   Super Admin can open Sidebar editor for a **system** role and save; assignees linked to that role row should get `rbac_role` navigation from stored ids.

7. **HR**  
   HR can open Management → Groups, see staff table, use RBAC panel with staff dropdown + custom roles list (`GET /users/rbac/custom-roles`). HR cannot open Admin → Roles.

8. **JWT vs nav mismatch**  
   A user may see a link their `UserRole` cannot call (403). Documented product risk; optional future: intersect nav with enum-based max set on save/read.
