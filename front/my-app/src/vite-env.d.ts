/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string
  readonly VITE_APP_SURFACE?: string
  /** Base URL of company dashboard (e.g. https://app.example.com) for landing CTAs */
  readonly VITE_COMPANY_APP_URL?: string
  /** Base URL of provider portal (e.g. https://partners.example.com) for landing CTAs */
  readonly VITE_PROVIDER_APP_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
