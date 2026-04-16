# Ziyara Demo Accounts

One dummy account per **role** in the system. All use the same password. Accounts are created automatically on backend startup when the `prod` profile is **not** active (e.g. default or `dev`).

## Password (all accounts)

```
Demo123!
```

## Accounts by role

| Role | Email |
|------|--------|
| Customer | `customer@ziyarah.com` |
| Super Admin | `super_admin@ziyarah.com` |
| Sales Manager | `sales_manager@ziyarah.com` |
| Sales Representative | `sales_representative@ziyarah.com` |
| Finance Manager | `finance_manager@ziyarah.com` |
| Accountant | `accountant@ziyarah.com` |
| Support Manager | `support_manager@ziyarah.com` |
| Support Agent | `support_agent@ziyarah.com` |
| CEO | `ceo@ziyarah.com` |
| General Manager | `general_manager@ziyarah.com` |
| HR Manager | `hr_manager@ziyarah.com` |
| Provider Manager | `provider_manager@ziyarah.com` |
| Provider Finance | `provider_finance@ziyarah.com` |
| Provider Staff | `provider_staff@ziyarah.com` |
| Taxi Operator | `taxi_operator@ziyarah.com` |

## Quick copy

**Password:** `Demo123!`

**Emails:**
- `customer@ziyarah.com`
- `super_admin@ziyarah.com`
- `sales_manager@ziyarah.com`
- `sales_representative@ziyarah.com`
- `finance_manager@ziyarah.com`
- `accountant@ziyarah.com`
- `support_manager@ziyarah.com`
- `support_agent@ziyarah.com`
- `ceo@ziyarah.com`
- `general_manager@ziyarah.com`
- `hr_manager@ziyarah.com`
- `provider_manager@ziyarah.com`
- `provider_finance@ziyarah.com`
- `provider_staff@ziyarah.com`
- `taxi_operator@ziyarah.com`

## Notes

- **Super admin is permanent:** `super_admin@ziyarah.com` is created or password reset to `Demo123!` on **every** startup (any profile), unless you set `app.demo.super-admin.enabled=false` (e.g. in production).
- Other demo users (per role) are created only when the **prod** profile is **not** active (`DemoDataSeeder`).
- To disable all demo seeding in production, start with: `-Dspring.profiles.active=prod` (this also sets `app.demo.super-admin.enabled=false` via `application-prod.yml`).

## Troubleshooting login

- **Use exactly:** `super_admin@ziyarah.com` and `Demo123!` (password is case-sensitive).
- **Do not** start the backend with `-Dspring.profiles.active=prod` if you want demo users; with `prod` the seeder does not run and no demo accounts are created.
- If login still fails: check backend logs for "Demo user created" or "Demo super admin password reset" to confirm the seeder ran; ensure the backend is reachable (e.g. frontend `VITE_API_URL` points to the correct backend base URL).
