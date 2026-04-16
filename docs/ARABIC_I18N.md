# Arabic (_ar) and System-Wide Language Support

When the user changes the language (EN ↔ ع), the **entire system** switches: UI labels and API content both follow the selected language.

---

## How It Works

### Frontend
- **Locale** is stored in `localStorage` under `ziyarah_locale` (`en` or `ar`).
- **Language switcher** (EN | ع) in the main header toggles language.
- **UI strings** use `useLocale().t('key')` (e.g. `t('nav.home')`) from `src/context/LocaleContext.js` and translation files:
  - `src/locales/en.json`
  - `src/locales/ar.json`
- **RTL**: When locale is `ar`, the app sets `document.documentElement.dir = 'rtl'` and `lang="ar"`.
- **API**: Every request sends `Accept-Language: ar` or `Accept-Language: en` so the backend returns localized content.

### Backend
- **LocaleFilter** runs first and reads `Accept-Language`; it stores the resolved locale in **RequestLocaleHolder** (thread-local).
- **Entities** that have user-facing text can have `_ar` columns (e.g. `name_ar`, `description_ar`).
- **Responses** use `RequestLocaleHolder.localized(defaultValue, arValue)` when building DTOs so the API returns the correct language without changing the response shape (e.g. `name` is always present; its value is Arabic when the client asked for `ar`).

---

## Database: _ar Columns

Run the migration so tables have Arabic columns.

**Option A – PowerShell script (recommended)**  
From the repo root (with PostgreSQL running at localhost:5432):

```powershell
.\scripts\run-ar-migration.ps1
```

The script uses `psql` if available, otherwise Docker (`postgres:15`). Default DB: `ziyarah`, user: `ziyarah_user`, password: `ziyarah_password` (from `backend/src/main/resources/application.yml`).

**Option B – Manual**  
- With **psql** in PATH:  
  `$env:PGPASSWORD='ziyarah_password'; psql -h localhost -p 5432 -U ziyarah_user -d ziyarah -f database\migrations\010_ar_columns_i18n.sql`  
- With **Docker**:  
  `docker run --rm -e PGPASSWORD=ziyarah_password -v "%cd%\database\migrations:/migrations" postgres:15 psql -h host.docker.internal -p 5432 -U ziyarah_user -d ziyarah -f /migrations/010_ar_columns_i18n.sql`  
- Or open `database/migrations/010_ar_columns_i18n.sql` in pgAdmin/DBeaver and execute it.

**Tables/columns added:**
- `departments`: `name_ar`, `description_ar`
- `groups`: `name_ar`, `description_ar`
- `roles`: `name_ar`, `description_ar`
- `permissions`: `name_ar`, `description_ar`
- `services`: `name_ar`, `description_ar`
- `service_providers`: `company_name_ar`, `description_ar`
- `discount_codes`: `description_ar`

After migration, you can set Arabic values (e.g. in admin or seed data).

**Backend localized responses (when `Accept-Language: ar`):**
- **Departments** – GET /departments: `name`, `description`
- **Groups, Roles, Permissions** – GET /roles/groups, role/permission APIs: `name`, `description` (and group names in role responses)
- **Services** – GET /services, /services/search, /services/{id}: `name`, `description` (jOOQ ServiceQueryHandler)
- **Service providers** – GET /providers, /providers/{id}: `name` (from company_name_ar in JPA)
- **Discount codes** – GET /discounts, /discounts/{id}: `description` (jOOQ DiscountQueryHandler)

---

## Extending to More Entities

To have other endpoints return localized content:

1. **Domain entity**: Add `nameAr` / `descriptionAr` (or equivalent) with getters/setters.
2. **JPA entity**: Add `name_ar` / `description_ar` columns (already in migration for the tables above).
3. **Mapper**: Map the _ar fields from JPA to domain.
4. **Response DTO**: Keep a single `name` (and `description`). When building the DTO in the service or query layer, set:
   - `name = RequestLocaleHolder.localized(entity.getName(), entity.getNameAr())`
   - `description = RequestLocaleHolder.localized(entity.getDescription(), entity.getDescriptionAr())`

For jOOQ query handlers (e.g. services, discounts), add `F_NAME_AR` / `F_DESCRIPTION_AR` fields, select them, and use `RequestLocaleHolder.localized(r.get(F_NAME), r.get(F_NAME_AR))` in the `toResponse` method.

---

## Adding New UI Translations

1. Add the key to both `frontend/src/locales/en.json` and `frontend/src/locales/ar.json`.
2. In components, use `const { t } = useLocale();` and `t('section.key')`.

Example:

```json
// en.json
"booking": { "bookNow": "Book Now" }

// ar.json
"booking": { "bookNow": "احجز الآن" }
```

```js
const { t } = useLocale();
<button>{t('booking.bookNow')}</button>
```

---

## Summary

| Layer        | What changes with language |
|-------------|-----------------------------|
| **Frontend**| All strings from `t(...)`, RTL + `lang="ar"`, `Accept-Language` header |
| **Backend** | Response fields built with `RequestLocaleHolder.localized(en, ar)` (e.g. departments; extend to services, roles, etc. as above) |
| **Database**| Optional `_ar` columns; run `010_ar_columns_i18n.sql` and fill Arabic content where needed |

Changing the language in the UI (EN ↔ ع) updates the whole system: UI language, direction, and API content (for endpoints that use `RequestLocaleHolder` and _ar data).
