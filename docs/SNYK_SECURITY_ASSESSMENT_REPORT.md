# Security Assessment Report (SAST + SCA)

Date: 2026-03-26  
Workspace: `C:\Users\BL3T\Documents\GitHub\Ziyara`  
Scanner: Snyk MCP (`snyk_sca_scan`, `snyk_code_scan`)  
Scope: Entire workspace (all detected projects and source files)

## 1. Executive Summary

- Total vulnerabilities discovered: **154**
  - **SCA (dependencies): 73**
  - **SAST (source code): 81**
- Severity breakdown (combined):
  - **Critical: 9**
  - **High: 35**
  - **Medium: 31**
  - **Low: 79**

Severity breakdown by scan type:

| Scan Type | Critical | High | Medium | Low | Total |
|---|---:|---:|---:|---:|---:|
| SCA | 9 | 33 | 20 | 11 | 73 |
| SAST | 0 | 2 | 11 | 68 | 81 |

---

## 2. Dependency Vulnerabilities (SCA)

Notes:
- Exploit maturity is **not explicitly provided** in Snyk MCP output for these findings.
- Target remediation is shown from Snyk `fixedIn` and/or package upgrade advice.

| Package | Current Version | CVE/CWE | Severity | Exploit Maturity | Required Target Version / Remediation |
|---|---|---|---|---|---|
| `org.apache.tomcat.embed:tomcat-embed-core` | `10.1.18` | Multiple CVEs incl. `CVE-2025-66614`; CWE-295/23/426/770/863/etc. | Critical/High/Medium/Low | Not provided | Upgrade via `spring-boot-starter-web` to chain resolving to Tomcat `10.1.50+` (best current signal from scan: starter `3.4.13+`) |
| `org.springframework.security:spring-security-web` | `6.2.1` | `CVE-2026-22732`, `CVE-2024-38821`, `CVE-2024-22234`, `CVE-2024-38827`; CWE-524/862/287/285 | Critical/High/Medium | Not provided | Upgrade to `6.5.9+` (or newer Spring Boot BOM chain) |
| `org.springframework.security:spring-security-crypto` | `6.2.1` | `CVE-2025-22228`, `CVE-2024-38827`; CWE-305/285 | Critical/Medium | Not provided | Upgrade to at least `6.3.8` (prefer latest compatible BOM) |
| `org.postgresql:postgresql` | `42.6.0` | `CVE-2024-1597`; CWE-89 | Critical | Not provided | Upgrade to `42.6.1+` (or higher in listed fixed branches) |
| `com.fasterxml.jackson.core:jackson-core` | `2.16.2` | CWE-770 | High | Not provided | Upgrade to `2.18.6+` (or `2.21.1+`) |
| `org.springframework:spring-webmvc` | `6.1.8` | `CVE-2024-38816`, `CVE-2024-38819`, `CVE-2026-22737`; CWE-22/23 | High | Not provided | Upgrade to `6.2.17+` (or Boot starter chain `3.5.12+`) |
| `org.springframework.boot:spring-boot-actuator-autoconfigure` | `3.2.2` | `CVE-2026-22733`, `CVE-2025-22235`; CWE-288/20 | High/Medium | Not provided | Upgrade actuator starter to `3.5.12+` |
| `org.springframework.boot:spring-boot-actuator` | `3.2.2` | `CVE-2026-22733`; CWE-288 | High | Not provided | Upgrade to `3.5.12+` |
| `org.springframework:spring-core` | `6.1.8` | `CVE-2025-41249`, `CVE-2024-38820`; CWE-863/178 | High/Low | Not provided | Upgrade to `6.2.11+` |
| `org.springframework:spring-beans` | `6.1.8` | `CVE-2025-41242`; CWE-23 | High | Not provided | Upgrade to `6.2.10+` |
| `org.springframework:spring-web` | `6.1.8` | `CVE-2025-41234`, `CVE-2024-38809`, `CVE-2026-22735`, `CVE-2024-38820`; CWE-113/625/74/178 | Medium/Low | Not provided | Upgrade to `6.2.17+` |
| `org.apache.commons:commons-compress` | `1.24.0` | `CVE-2024-25710`, `CVE-2024-26308`; CWE-835/770 | High/Medium | Not provided | Upgrade to `1.26.0+` |
| `org.apache.commons:commons-lang3` | `3.14.0` | `CVE-2025-48924`; CWE-674 | High | Not provided | Upgrade to `3.18.0+` |
| `net.minidev:json-smart` | `2.5.0` | `CVE-2024-57699`; CWE-400 | High | Not provided | Upgrade to `2.5.2+` |
| `org.assertj:assertj-core` | `3.24.2` | `CVE-2026-24400`; CWE-611 | High | Not provided | Upgrade to `3.27.7+` |
| `ch.qos.logback:logback-core` | `1.4.14` | `CVE-2025-11226`, `CVE-2026-1225`, `CVE-2024-12801`, `CVE-2024-12798`; CWE-454/918/138 | Medium/Low | Not provided | Upgrade to `1.5.25+` through newer Boot/Actuator chain |
| `ch.qos.logback:logback-classic` | `1.4.14` | `CVE-2024-12798`; CWE-138 | Medium | Not provided | Upgrade to `1.5.13+` |
| `com.jayway.jsonpath:json-path` | `2.8.0` | `CVE-2023-51074`; CWE-120 | Low | Not provided | Upgrade to `2.9.0+` |
| `org.xmlunit:xmlunit-core` | `2.9.1` | `CVE-2024-31573`; CWE-453 | Medium | Not provided | Upgrade to `2.10.0+` |
| `flatted` | `3.4.1` | `CVE-2026-33228`; CWE-1321 | Critical | Not provided | Upgrade to `3.4.2+` (relock npm deps if transitive lock prevents) |
| `picomatch` | `4.0.3` | `CVE-2026-33671`, `CVE-2026-33672`; CWE-1333/1321 | High/Medium | Not provided | Upgrade to `4.0.4+` (or patched `2.3.2/3.0.2` lines if pinned) |
| `undici` | `7.22.0` | `CVE-2026-1525/1526/1527/1528/2229/2581`; CWE-444/409/93/248/770 | High/Medium | Not provided | Upgrade to `7.24.0+` (or `6.24.0+` branch) |

Primary manifests impacted:
- `core/build.gradle.kts`
- `front/my-app/package.json`

---

## 3. Source Code Vulnerabilities (SAST)

The scan returned 81 findings. Many are repetitive instances of the same pattern. Below are the actionable groups with source-to-sink flow and remediation requirements.

### 3.1 Path Traversal (High)

1) **Portal upload flow**
- Type: Path Traversal (`CWE-23`)
- Location: `core/src/main/java/com/ziyara/backend/presentation/controller/PortalController.java` (around line 152)
- Data flow (source -> sink):
  - HTTP request parameter (filename/path segment) in `PortalController`
  - forwarded to `PortalService`/`ServiceImageService`
  - reaches `LocalMediaStorageService`
  - finally written via `java.nio.file.Files.write(...)`
- Required remediation:
  - Canonicalize and normalize (`Path.normalize().toAbsolutePath()`)
  - Reject `..`, absolute paths, and separator injection
  - Enforce strict allowlist for filename chars/extensions
  - Verify resolved path stays under media root (`resolved.startsWith(baseRoot)`)
  - Generate server-side storage names instead of trusting client filenames

2) **Service upload flow**
- Type: Path Traversal (`CWE-23`)
- Location: `core/src/main/java/com/ziyara/backend/presentation/controller/ServiceController.java` (around line 226)
- Data flow: same pattern as above to `Files.write(...)` in storage layer
- Required remediation: same hardened path-handling controls as above

### 3.2 DOM-based XSS (Medium)

1) Landing service detail rendering
- Type: DOM XSS (`CWE-79`)
- Location: `front/my-app/src/apps/landing/LandingServiceDetailPage.tsx` (around line 100)
- Data flow:
  - React state derived from route/query/content data
  - value flows into dynamic HTML/script-sensitive rendering
- Required remediation:
  - Never use unsanitized HTML sinks
  - Sanitize rich HTML with a robust sanitizer before rendering
  - Prefer React escaped rendering, avoid `dangerouslySetInnerHTML` for untrusted data

2) Service detail page
- Type: DOM XSS (`CWE-79`)
- Location: `front/my-app/src/pages/services/ServiceDetailPage.tsx` (around line 134)
- Data flow: route/state input -> dynamic render sink
- Required remediation: same output-encoding and sanitization controls

### 3.3 Open Redirect (Medium)

1) Login redirect target
- Type: Open Redirect (`CWE-601`)
- Location: `front/my-app/src/pages/LoginPage.tsx` (around line 50)
- Data flow:
  - `location` state/param supplies redirect target
  - value passed to navigation call
- Required remediation:
  - allowlist internal paths only
  - block absolute URLs, protocol-relative URLs, and external origins
  - fallback to a fixed safe default (e.g., `/dashboard`)

2) ServiceType redirect parameter handling
- Type: Open Redirect (`CWE-601`)
- Location: `front/my-app/src/pages/services/ServiceTypePage.tsx` (around line 106)
- Data flow: route param -> navigation sink
- Required remediation: same allowlist + fallback approach

### 3.4 Hardcoded Passwords (Medium/Low)

1) Backend seeders
- Type: Hardcoded password (`CWE-798`, `CWE-259`)
- Locations:
  - `core/src/main/java/com/ziyara/backend/infrastructure/config/DemoDataSeeder.java` (around line 26)
  - `core/src/main/java/com/ziyara/backend/infrastructure/config/SuperAdminSeeder.java` (around line 27)
- Data flow: constant password literal -> account provisioning logic
- Required remediation:
  - remove hardcoded values from code
  - load secrets from environment or secret manager
  - generate one-time random bootstrap password and force rotation

2) Frontend translation literals (false-positive-prone but flagged)
- Type: Hardcoded password-like strings
- Location: `front/my-app/src/i18n/translations.ts` (multiple lines, e.g., ~342, ~1058, ~1259)
- Required remediation:
  - replace realistic password literals in examples/messages with neutral placeholders
  - avoid embedding credential-shaped strings

### 3.5 CSRF-related Findings (mostly Low; broad pattern)

- Type: Spring CSRF / CSRF disabled patterns (`CWE-352`)
- Locations: many controller endpoints and tests, plus security config signals
  - Example: `core/src/main/java/com/ziyara/backend/infrastructure/config/SecurityConfig.java` (`.csrf(AbstractHttpConfigurer::disable)`)
- Data flow pattern:
  - state-changing HTTP endpoints under cookie-auth risk model
  - CSRF defense absent/disabled in config path
- Required remediation:
  - if using cookies/session auth: enable CSRF tokens for state-changing endpoints
  - if using stateless JWT-only API: document/auth-enforce bearer-only usage and disable cookie auth surfaces
  - scope CSRF ignores narrowly rather than global disable

---

## 4. Architectural Security Posture

### 4.1 Hardcoded secrets / misconfigured environment

Identified issues:
- `docker-compose.yml` includes plaintext credentials/default secrets:
  - `POSTGRES_PASSWORD: ziyarah_password`
  - `SPRING_DATASOURCE_PASSWORD: ziyarah_password`
  - `JWT_SECRET: your-256-bit-secret-key-for-jwt-token-generation-please-change-in-production`
  - `PGADMIN_DEFAULT_PASSWORD` fallback default
- Demo passwords are hardcoded in backend seeders (`Demo123!`).

Risk:
- Secret leakage via repository/history, easy credential reuse, weak production hardening.

Required architecture changes:
- Move all secrets to environment-injected runtime values or secret manager (Vault/KMS/etc.)
- Remove insecure defaults from committed compose files
- Enforce startup failure when critical secrets are missing or placeholder values are used
- Separate dev/demo seed logic from production deploy artifacts

### 4.2 Authentication/authorization bypass risks in domain/API logic

High-risk indicators from scan data:
- Multiple dependency CVEs indicating **authentication/authorization bypass** in Spring Security/Tomcat stack.
- Broad CSRF findings indicate potential mismatch between auth model and request protections.
- Open redirect flows increase phishing/session abuse risk in frontend login/navigation logic.

Architecture recommendations:
- Prioritize framework dependency upgrades (Spring Boot/Security/Tomcat) before feature work
- Add centralized redirect validation utility in frontend router layer
- Add security regression tests:
  - privilege boundary tests per role
  - CSRF behavior tests for all state-changing endpoints
  - path traversal negative tests for media upload/storage APIs

---

## Scan Artifacts

- SCA raw output: `C:\Users\BL3T\.cursor\projects\c-Users-BL3T-Documents-GitHub-Ziyara\agent-tools\6f76bc6c-83d8-4c45-8180-a2f7aed1156f.txt`
- SAST raw output: `C:\Users\BL3T\.cursor\projects\c-Users-BL3T-Documents-GitHub-Ziyara\agent-tools\5a8e8ac7-72ae-4ee5-bcba-8d619d788d79.txt`

No automatic fixes were applied.
