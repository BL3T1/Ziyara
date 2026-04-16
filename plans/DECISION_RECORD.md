# Implementation Plan – Decision Record (Phase 0)

One-page record of decisions that unblock Phases 1–4. *Ref: [MISSING_IMPLEMENTATION_PLAN.md](./MISSING_IMPLEMENTATION_PLAN.md) Phase 0.*

---

## 1. Canonical app

| Decision | Choice |
|----------|--------|
| **Company Dashboard** | `front/my-app` (Vite). |
| **Client Portal** | **Option A:** Same app at `/portal` (single SPA, two route trees). One design system and shared auth. |
| **CRA (`frontend/`)** | Keep as reference until Phase 5 (Vite parity) is complete; then deprecate or archive. |

---

## 2. Design & Figma

| Decision | Choice |
|----------|--------|
| **Design baseline** | Align with [DASHBOARD_DESIGN_REPORT.md](./DASHBOARD_DESIGN_REPORT.md): Primary Royal Blue (#1A237E), Success/Warning/Danger, dark/light theme, glassmorphism optional. |
| **Tokens** | Document in `front/my-app/PROJECT_CONTEXT.md` or design-tokens doc when wiring theme (Phase 5). |
| **Figma** | FIGMA_NEEDS_BY_PHASE checklist: theme toggle, search modal, language switcher, refund modal, provider dashboard—implement in code per plan where designs are not yet provided. |

---

## 3. Payment gateway

| Decision | Choice |
|----------|--------|
| **Provider** | **TBD** – Select (e.g. Stripe, Flutterwave, or Visa Direct partner). Document in PAYMENT_METHODS or tech spec. |
| **Phase 2** | Obtain sandbox API keys and webhook signing secret before implementing adapter and webhook verification. |

---

## 4. Database & backend

| Decision | Choice |
|----------|--------|
| **Phase 1** | Add payment columns (gateway_reference, three_ds_status, gateway_response) and optional commission audit table; refund reason mandatory in API; audit log on refund. |
| **Table prefix migration (Phase 4)** | Separate, high-risk phase with backup and rollback plan; not before Phase 3 modular layout. |

---

## 5. Summary

- **Canonical app:** Vite `front/my-app`; portal at `/portal` (Option A).
- **CRA:** Retain until Phase 5 complete.
- **Gateway:** TBD; sandbox keys required for Phase 2.
- **Design:** DASHBOARD_DESIGN_REPORT baseline; code-first where Figma pending.
- **No code changes required in Phase 0;** this document unblocks Phases 1–4.

*Version 1.0 | Phase 0 complete.*
