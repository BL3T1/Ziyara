# Payment Methods & Visa Integration: Ziyarah Platform

This report details the comprehensive strategy for integrating Visa card payments and other payment methods within the Ziyarah Modular Monolith.

## 1. Supported Payment Methods

| Method | Type | Processing | Best For |
| :--- | :--- | :--- | :--- |
| **Visa / Credit Card** | **Digital** | Real-time Gateway | Dynamic pricing, instant confirmation |
| **Bank Transfer** | Manual | Verification required | High-value bookings (Long-stay hotels) |
| **Wallet (Z-Pay)** | Digital | Internal balance | Frequent users, refunds/compensations |
| **Cash on Service** | Manual | At property/taxi | Restaurants, last-minute Taxis |

---

## 2. Detailed Visa Integration Stack

To achieve a "Pro" standard, the Visa integration follows a secure, decoupled architecture.

### 2.1 Frontend Components (Client-Side)
- **Secure Payment UI**: Dedicated checkout pages for web and mobile.
- **Gateway SDK/Library**: Integration of libraries like *Stripe.js* or *Flutterwave Inline* to capture and tokenize card data directly, ensuring sensitive info never touches the Ziyarah server.
- **3D Secure (3DS) Logic**: Integrated redirection handlers for bank-side authentication and callback processing.

### 2.2 Backend Infrastructure (Server-Side)
- **Modular API Client**: Encapsulated within the `pay_` module to communicate with Gateway endpoints via secure SDKs or HTTP (Axios/Internal WebClient).
- **Secure Webhook Endpoint**: Publicly accessible URL (`/api/v1/pay/webhooks`) to handle asynchronous events (success, failure, chargebacks).
- **Environment Configuration**: Explicit use of `.env` files and Secrets Manager for API keys and signing secrets.
- **Persistence Layer**: Specialized `pay_` tables for transaction status, customer references, and **Idempotency Keys** to prevent duplicate charges.

### 2.3 Security & Compliance
- **Mandatory HTTPS**: Encrypted communication across all tiers.
- **Reduced PCI Scope**: Achieved via tokenization/hosted fields, supplemented by secure coding practices and access controls.
- **Signature Verification**: Mandatory validation of all inbound webhook payloads against the Gateway's signing certificate.
  - **Implementation (Phase 2):** `WebhookSignatureFilter` verifies `X-Webhook-Signature` (or `Stripe-Signature`) with HMAC-SHA256 using `app.payment.gateway.webhook-secret`. Invalid signature returns 403. Algorithm: `HMAC-SHA256(secret, rawRequestBody)`; header value is hex-encoded. Idempotent processing by `gateway_reference` to avoid duplicate application of callbacks.

### 2.4 Development & Optional Enhancements
- **Sandbox Testing**: Use of mock card numbers and sandbox API keys for full cycle testing.
- **Local Tunneling**: Tools like *ngrok* for local webhook testing.
- **Idempotency Layer**: Shared logic to catch and deduplicate retried payment requests.
- **Queue System**: Utilization of internal Spring events or Kafka to process webhooks without blocking the main thread.

---

## 3. Financial Controls

- **Reconciliation**: Automated nightly jobs cross-reference `pay_transactions` with Gateway logs.
- **Fraud Detection**: Velocity checks on the same card within a 15-minute window.

---
*Report refined by Antigravity.*
