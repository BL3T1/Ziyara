# Penetration Test & Vulnerability Report — Ziyara Monorepo

**Scan scope:** Full workspace (`core/`, `front/my-app/`, `database/`, `docker-compose.yml`, docs, etc.)  
**Tools (Snyk MCP):** `snyk_sca_scan` (`all_projects: true`, `dev: true`, `show_vulnerable_paths: all`), `snyk_code_scan`, `snyk_iac_scan`  
**Supplementary CLI check:** `snyk iac test` on `core/` (same outcome as MCP for that path)  
**Date:** 2026-04-04  

---

## 1. Executive Summary

### 1.1 Totals by severity (all Snyk findings from this run)

| Severity  | SCA (OSS) | SAST (Code) | IaC | **Total** |
|-----------|------------|-------------|-----|-----------|
| **Critical** | 0 | 0 | — | **0** |
| **High**     | 3 | 0 | — | **3** |
| **Medium**   | 1 | 9 | — | **10** |
| **Low**      | 0 | 5 | — | **5** |
| **Total**    | **4** | **14** | **0** *(scan failed — see §3)* | **18** |

**Notes on counting**

- **SCA:** 4 distinct issues (2 on `commons-compress` = 1 High + 1 Medium).  
- **SAST:** 14 distinct issues (several are **test-only** or **i18n UI strings**; see exploit vectors).  
- **IaC:** No issues ingested; automated **IaC test did not successfully analyze** this workspace layout (§3).

---

## 2. Vulnerability Detail & Remediation

### 2.1 Open-source / dependency analysis (SCA) — `snyk_sca_scan`

Snyk reports issues against the **manifest** (not a single vulnerable line of application code). Below, **file path** is the manifest Snyk attributed; **line** refers to the **dependency block** where versions are controlled unless a lockfile line is cited.

---

#### Finding SCA-1

| Field | Value |
|--------|--------|
| **Title** | Allocation of Resources Without Limits or Throttling |
| **Snyk ID** | `SNYK-JAVA-COMFASTERXMLJACKSONCORE-15365924` |
| **Severity** | **High** |
| **Package** | `com.fasterxml.jackson.core:jackson-core` **2.19.4** (Maven) |
| **CWE** | **CWE-770** |
| **CVE** | *(Not returned in this Snyk JSON payload; use Snyk UI / `snyk test` verbose for linked CVE if assigned.)* |
| **File path** | `core/build.gradle.kts` |
| **Line (reference)** | Dependency management block **lines 39–71** (transitive via Spring/Jackson stack). |

**Exploit vector:** Attacker supplies **untrusted input** processed by Jackson (JSON/XML or related streaming) in a way that triggers **resource exhaustion** (e.g., CPU/memory) — classically **denial of service**, not necessarily RCE by itself.

**Remediation plan**

- **Upgrade path (Snyk):** `jackson-core` **2.21.1** (also lists **2.18.6** as fixed branch; align with your Spring Boot BOM).  
- **Gradle refinement (example — align Jackson patch):**

```kotlin
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.12"))
    // After verifying compatibility with Spring Boot 3.5.12:
    implementation("com.fasterxml.jackson.core:jackson-core:2.21.1")
}
```

Or extend existing `resolutionStrategy.eachDependency` to force `com.fasterxml.jackson.core:jackson-core` (and related `jackson-*` modules) to **2.21.1** after `./gradlew dependencyInsight` confirms the tree.

---

#### Finding SCA-2

| Field | Value |
|--------|--------|
| **Title** | Infinite loop |
| **Snyk ID** | `SNYK-JAVA-ORGAPACHECOMMONS-6254296` |
| **Severity** | **High** |
| **Package** | `org.apache.commons:commons-compress` **1.24.0** |
| **CWE** | **CWE-835** |
| **CVE** | **CVE-2024-25710** |
| **File path** | `core/build.gradle.kts` |
| **Line (reference)** | **Lines 39–71** (transitive; commonly via **testcontainers** / archive tooling). |

**Exploit vector:** Malicious **archive** (e.g., crafted compression or format) parsed by `commons-compress` can cause **infinite loop** → **CPU exhaustion (DoS)**. Risk is **highest** if this library is on the **runtime** classpath and parses **user-controlled** archives; **lower** if **test-only**.

**Remediation plan**

- **Upgrade path:** `commons-compress` **≥ 1.26.0** (Snyk `fixedIn`: `1.26.0`).  
- Snyk’s text suggests upgrading `spring-boot-testcontainers` to **4.x**; **do not blindly jump major Spring Boot** — prefer **forcing** the vulnerable artifact:

```kotlin
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.module.toString() == "org.apache.commons:commons-compress") {
            useVersion("1.26.0")
            because("CVE-2024-25710 / CVE-2024-26308")
        }
    }
}
```

- **Verify scope:** `./gradlew dependencies --configuration runtimeClasspath` vs `testRuntimeClasspath` to confirm whether **1.24.0** appears at **runtime**.

---

#### Finding SCA-3

| Field | Value |
|--------|--------|
| **Title** | Allocation of Resources Without Limits or Throttling |
| **Snyk ID** | `SNYK-JAVA-ORGAPACHECOMMONS-6254297` |
| **Severity** | **Medium** |
| **Package** | `org.apache.commons:commons-compress` **1.24.0** |
| **CWE** | **CWE-770** |
| **CVE** | **CVE-2024-26308** |
| **File path** | `core/build.gradle.kts` |
| **Line (reference)** | **Lines 39–71** |

**Exploit vector:** Similar to SCA-2: **untrusted archive** → **resource exhaustion / DoS** via parser behavior.

**Remediation plan:** Same as **SCA-2** (bump **`commons-compress` to ≥ 1.26.0**).

---

#### Finding SCA-4

| Field | Value |
|--------|--------|
| **Title** | Infinite loop |
| **Snyk ID** | `SNYK-JS-BRACEEXPANSION-15789759` |
| **Severity** | **High** |
| **Package** | `brace-expansion` **1.1.12** (npm) |
| **CWE** | **CWE-835** |
| **CVE** | **CVE-2026-33750** |
| **File path** | `front/my-app/package.json` |
| **Lockfile evidence** | `front/my-app/package-lock.json` — `node_modules/brace-expansion` resolved to **1.1.12** (e.g. lockfile section around **lines 2788–2790** region). |

**Exploit vector:** Dependency uses `brace-expansion` when expanding **glob** patterns; a **pathological pattern** can trigger **infinite loop** → **DoS** in **build tooling** or **CLI tools** that use the vulnerable chain (severity depends on whether user-controlled strings reach that code — often **dev/CI** or **build-time**).

**Remediation plan**

- `npm update` / refresh lockfile so no tree resolves **1.1.12** where avoidable.  
- If a parent package pins an old range, use **npm overrides** (npm 8.3+):

```json
{
  "overrides": {
    "brace-expansion": "2.0.3"
  }
}
```

(Snyk lists fixed lines including **1.1.13**, **2.0.3**, **3.0.2**, **5.0.5** — pick one compatible with your tree after `npm ls brace-expansion`.)

---

### 2.2 Static application security testing (SAST) — `snyk_code_scan`

---

#### Finding CODE-1

| Field | Value |
|--------|--------|
| **Title** | Cross-Site Request Forgery (CSRF) |
| **Rule** | `java/DisablesCSRFProtection/test` |
| **Severity** | **Low** |
| **CWE** | **CWE-352** |
| **CVE** | N/A (SAST) |
| **File** | `core/src/test/java/com/ziyara/backend/presentation/controller/EndpointFunctionalTest.java` |
| **Line** | **53** (`csrf.disable()` on test `SecurityFilterChain`) |
| **Column** | 13 |

**Exploit vector:** In **production**, disabled CSRF on **cookie-session** flows can allow **cross-site state-changing** requests. Here the chain is inside **`@TestConfiguration`** for **functional tests** — **not** the shipped production `SecurityFilterChain`.

**Remediation plan**

- **No production code change required** if production config enables CSRF appropriately for session-based flows.  
- **Reduce noise:** Add a **Snyk Code ignore** with justification “test-only security override,” or extract shared test security to a class annotated/documented as **test-only**.  
- **Optional pattern** (keep test permissive but explicit):

```java
// Test-only: mirrors production matchers but disables CSRF for RestTemplate-based tests.
http.securityMatcher("/**")
    .csrf(csrf -> csrf.disable())
```

---

#### Finding CODE-2

| Field | Value |
|--------|--------|
| **Title** | Cross-Site Request Forgery (CSRF) |
| **Rule** | `java/DisablesCSRFProtection/test` |
| **Severity** | **Low** |
| **CWE** | **CWE-352** |
| **File** | `core/src/test/java/com/ziyara/backend/presentation/controller/EndpointFunctionalTest.java` |
| **Line** | **63** |
| **Column** | 13 |

**Exploit vector / remediation:** Same as **CODE-1** (second `SecurityFilterChain` bean in the same test config).

---

#### Finding CODE-3

| Field | Value |
|--------|--------|
| **Title** | Cross-Site Request Forgery (CSRF) |
| **Rule** | `java/DisablesCSRFProtection/test` |
| **Severity** | **Low** |
| **CWE** | **CWE-352** |
| **File** | `core/src/test/java/com/ziyara/backend/presentation/controller/OpenApiEndpointSmokeTest.java` |
| **Line** | **137** |
| **Column** | 13 |

**Exploit vector:** Test override disables CSRF for **OpenAPI smoke** wiring (see comment **lines 128–130** in file). **Not production.**

**Remediation plan:** Snyk ignore + ensure **production** `SecurityConfig` does not use this `TestConfiguration`.

---

#### Finding CODE-4

| Field | Value |
|--------|--------|
| **Title** | Cross-Site Request Forgery (CSRF) |
| **Rule** | `java/DisablesCSRFProtection/test` |
| **Severity** | **Low** |
| **CWE** | **CWE-352** |
| **File** | `core/src/test/java/com/ziyara/backend/presentation/controller/OpenApiEndpointSmokeTest.java` |
| **Line** | **152** |
| **Column** | 13 |

**Exploit vector / remediation:** Same as **CODE-3**.

---

#### Finding CODE-5

| Field | Value |
|--------|--------|
| **Title** | Use of Hardcoded Passwords |
| **Rule** | `java/HardcodedPassword/test` |
| **Severity** | **Low** |
| **CWE** | **CWE-798**, **CWE-259** |
| **File** | `core/src/test/java/com/ziyara/backend/presentation/controller/EndpointFunctionalTest.java` |
| **Line** | **109** |
| **Column** | 31 |

**Exploit vector:** **None in production** — literal `Customer123!` is a **test credential** for generated test users.

**Remediation plan**

- Load test password from **test `application.yml`** or **env** (e.g. `System.getenv("E2E_TEST_PASSWORD")`) to satisfy scanners.  
- Or **Snyk ignore** with rationale “synthetic test password.”

---

#### Finding CODE-6

| Field | Value |
|--------|--------|
| **Title** | DOM-based Cross-site Scripting (XSS) |
| **Rule** | `javascript/DOMXSS` |
| **Severity** | **Medium** |
| **CWE** | **CWE-79** |
| **File** | `front/my-app/src/apps/landing/LandingServiceDetailPage.tsx` |
| **Line (sink)** | **104** (column 36) |
| **Dataflow lines** | **21, 91, 94, 98, 101, 103** (per Snyk) |

**Exploit vector:** Untrusted **`imageUrl`** (or related state) reaching **`img src`**. If **`javascript:`** or **`data:`** URLs were allowed, **script execution** could occur. Your code uses **`safeImageUrl(...)`** before `src` — Snyk may not fully model that sanitizer.

**Remediation plan**

- **Verify** every `img`/`iframe`/`a href` sink uses **`safeImageUrl`** or stricter **allowlist** (same-origin media only).  
- **Hardening snippet** (conceptual — already partially present):

```tsx
const src = safeImageUrl(item.imageUrl)
return src ? <img src={src} alt="" ... /> : null
```

- Add **unit tests** in `safeRendering.test.ts` for any new URL shapes.  
- If policy allows only **relative** media paths, **remove** `http(s)` allowance from `safeImageUrl`.

---

#### Finding CODE-7

| Field | Value |
|--------|--------|
| **Title** | DOM-based Cross-site Scripting (XSS) |
| **Rule** | `javascript/DOMXSS` |
| **Severity** | **Medium** |
| **CWE** | **CWE-79** |
| **File** | `front/my-app/src/pages/services/ServiceDetailPage.tsx` |
| **Line (sink)** | **137** (column 31) |
| **Dataflow lines** | **27, 123, 126, 130, 133, 136** |

**Exploit vector / remediation:** Same pattern as **CODE-6** (`safeImageUrl` at sink).

---

#### Finding CODE-8

| Field | Value |
|--------|--------|
| **Title** | Use of Hardcoded Passwords |
| **Rule** | `javascript/NoHardcodedPasswords` |
| **Severity** | **Medium** |
| **CWE** | **CWE-798**, **CWE-259** |
| **File** | `front/my-app/src/i18n/translations.ts` |
| **Line** | **361** (column 5) |

**Exploit vector:** **None** — value is **UI label** text (`passwordLabel`), not a secret.

**Remediation plan**

- **Snyk Code ignore** for `translations.ts` with comment.  
- Or rename key/value pattern to avoid substring `password` if you want zero noise (e.g. `accountSecretLabel`) — **purely cosmetic** for scanners.

---

#### Finding CODE-9

| Field | Value |
|--------|--------|
| **Title** | Use of Hardcoded Passwords |
| **Rule** | `javascript/NoHardcodedPasswords` |
| **Severity** | **Medium** |
| **CWE** | **CWE-798**, **CWE-259** |
| **File** | `front/my-app/src/i18n/translations.ts` |
| **Line** | **1375** |

**Exploit vector / remediation:** Same as **CODE-8** (duplicate locale block).

---

#### Finding CODE-10

| Field | Value |
|--------|--------|
| **Title** | Use of Hardcoded Passwords |
| **Rule** | `javascript/NoHardcodedPasswords` |
| **Severity** | **Medium** |
| **CWE** | **CWE-798**, **CWE-259** |
| **File** | `front/my-app/src/i18n/translations.ts` |
| **Line** | **370** |

**Exploit vector:** **None** — error string **`errPassword`** (validation message).

**Remediation plan:** Same as **CODE-8** (ignore or rename).

---

#### Finding CODE-11

| Field | Value |
|--------|--------|
| **Title** | Use of Hardcoded Passwords |
| **Rule** | `javascript/NoHardcodedPasswords` |
| **Severity** | **Medium** |
| **CWE** | **CWE-798**, **CWE-259** |
| **File** | `front/my-app/src/i18n/translations.ts` |
| **Line** | **1384** |

**Exploit vector / remediation:** Same as **CODE-10**.

---

#### Finding CODE-12

| Field | Value |
|--------|--------|
| **Title** | Use of Hardcoded Passwords |
| **Rule** | `javascript/NoHardcodedPasswords` |
| **Severity** | **Medium** |
| **CWE** | **CWE-798**, **CWE-259** |
| **File** | `front/my-app/src/i18n/translations.ts` |
| **Line** | **1682** |

**Exploit vector / remediation:** Same class of **false semantic positive** as **CODE-8**; ignore or rename.

---

#### Finding CODE-13

| Field | Value |
|--------|--------|
| **Title** | Open Redirect |
| **Rule** | `javascript/OR` |
| **Severity** | **Medium** |
| **CWE** | **CWE-601** |
| **File** | `front/my-app/src/pages/LoginPage.tsx` |
| **Line** | **51** (column 7) |
| **Dataflow** | From **`location`** (lines **37**, **50** per Snyk) |

**Exploit vector:** If **`navigate(...)`** accepted **attacker-controlled external URL**, phishing / token theft could follow. Current code uses **`safeRedirect(location.pathname, '/dashboard')`** — **pathname** is typically a **same-origin path** (not full URL).

**Remediation plan**

- **Audit** `safeRedirect` for edge cases (`//evil.com`, encoded slashes, etc.).  
- **Ensure** all `navigate` targets go through **`safeRedirect`** or a **path allowlist**.  
- Reference implementation already in `front/my-app/src/utils/safeRedirect.ts`.

---

#### Finding CODE-14

| Field | Value |
|--------|--------|
| **Title** | Open Redirect |
| **Rule** | `javascript/OR` |
| **Severity** | **Medium** |
| **CWE** | **CWE-601** |
| **File** | `front/my-app/src/pages/services/ServiceTypePage.tsx` |
| **Line** | **107** (column 34) |
| **Dataflow** | **34, 36, 106** |

**Exploit vector:** **`category`** from route params embedded in **`/${category}/${service.id}`** then passed to **`navigate(safeRedirect(...))`**. Risk depends on whether **`category`** can contain **`..`** or unusual segments; **`safeRedirect`** rejects `http://`, `//`, etc.

**Remediation plan**

- **Allowlist** `category` against known service types before navigation.  
- **Example:**

```tsx
const safeCategory = allowedCategories.has(category) ? category : 'services'
navigate(safeRedirect(`/${safeCategory}/${service.id}`, '/dashboard'))
```

---

## 3. Infrastructure-as-code (IaC) — `snyk_iac_scan` / `snyk iac test`

### 3.1 Full-workspace IaC scan (MCP)

| Field | Value |
|--------|--------|
| **Command equivalent** | `snyk iac test` on `c:\Users\BL3T\Documents\GitHub\Ziyara` |
| **Result** | **FAILED** — **SNYK-CLI-0012** |
| **Reason** | Snyk attempted to parse **JSON** files under `front/my-app` (including **`node_modules/**/tsconfig.json`**) and **failed to parse** them in this context. |

### 3.2 Targeted retries

| Path | Result |
|------|--------|
| `core/` (MCP + CLI) | **SNYK-CLI-0012** — **Could not find any valid IaC files** (Dockerfile alone not treated as IaC in this run). |
| Repo inventory | **No** `*.tf`, **no** Kubernetes `deployment` YAMLs found. |
| `docker-compose.yml` | Present at repo root; **CLI** `snyk iac test docker-compose.yml` **previously** returned “no valid IaC files” (Compose spec / product support). |

### 3.3 Manual IaC / Docker posture items (not from Snyk IaC engine)

Because **automated IaC results are empty**, record these for **manual** review:

- **`docker-compose.yml`** — env-required secrets (`POSTGRES_PASSWORD`, `JWT_SECRET`), **published ports**, **volume** mounts, **profiles**, inter-service trust.  
- **`core/Dockerfile`**, **`front/my-app/Dockerfile`**, **`database/Dockerfile`** — **non-root** users, **base image** freshness, **healthchecks**.  
- **`database/Dockerfile`** — **`ENV POSTGRES_PASSWORD=ziyarah_password`** default in image layers vs **compose** override (ensure production never relies on image default).

---

## 4. Security Posture Assessment

| Dimension | Assessment |
|-----------|------------|
| **Dependency hygiene (Java)** | **Moderate risk:** **High** issues in **Jackson** and **commons-compress** need **explicit version alignment** and **classpath verification** (runtime vs test). |
| **Dependency hygiene (npm)** | **Moderate risk:** **High** **CVE-2026-33750** on transitive **`brace-expansion@1.1.12`** — address via **lockfile refresh** and/or **`overrides`**. |
| **Application code (SAST)** | **Low–moderate effective risk:** Several **Medium** findings are **mitigated** (`safeRedirect`, `safeImageUrl`) or are **false semantic positives** (i18n “password” strings). **Test-only** **Low** findings do **not** reflect production. |
| **Infrastructure-as-code** | **Unassessed by Snyk in this workspace** due to **SNYK-CLI-0012** and **no recognized IaC files** under `core/`; **manual** review of **Docker/Compose** is **required** for a complete posture. |
| **Overall risk level** | **MODERATE** — **prioritize SCA High** and **confirm** SAST items that are **not** already guarded by your **URL helpers**; **complete** IaC coverage via **process change** (isolated IaC directory, `.snyk` ignore for `node_modules`, or upgraded Snyk policy) **plus** manual compose/Docker review. |

---

## 5. Recommended re-test sequence (after remediation)

1. `snyk_sca_scan` — `path`: workspace root, `all_projects`: true, `dev`: true.  
2. `snyk_code_scan` — `path`: workspace root.  
3. `snyk_iac_scan` — after **excluding** `node_modules` from traversal (e.g., dedicated `infra/` folder with only Compose/K8s/Terraform) or when Snyk supports your **Compose** format without choking on **`tsconfig.json`** under `node_modules`.  

---

*This report is a **baseline from Snyk automated scanning**; it does **not** replace manual penetration testing, threat modeling, or production configuration review.*
