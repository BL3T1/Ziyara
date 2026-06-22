import type { OpenAPISpec } from '../types/openapi'

function isOpenApiDocument(data: unknown): data is OpenAPISpec {
  if (!data || typeof data !== 'object') return false
  const o = data as Record<string, unknown>
  return typeof o.openapi === 'string' && o.openapi.startsWith('3.')
}

/**
 * Returns the full API base URL including the servlet context path (e.g. /api/v1).
 * Springdoc serves its spec relative to the context path, so we must include it.
 */
function getApiBase(): string {
  const apiUrl = import.meta.env.VITE_API_URL as string | undefined
  const trimmed = (apiUrl ?? '').replace(/\/$/, '')
  if (!trimmed) {
    return 'http://localhost:8080'
  }
  if (trimmed.startsWith('/')) {
    // Relative URL like /api/v1 — prepend current origin so fetch works
    return `${window.location.origin}${trimmed}`
  }
  try {
    const url = new URL(trimmed)
    const path = url.pathname === '/' ? '' : url.pathname.replace(/\/$/, '')
    return `${url.origin}${path}`
  } catch {
    return window.location.origin
  }
}

export async function fetchOpenApiSpecification(): Promise<OpenAPISpec> {
  const base = getApiBase()
  const candidates = [
    `${base}/api-docs`,
    `${base}/v3/api-docs`,
  ]
  const token = sessionStorage.getItem('token')
  const headers: Record<string, string> = token ? { Authorization: `Bearer ${token}` } : {}

  let lastErr: unknown
  for (const url of candidates) {
    try {
      const res = await fetch(url, { headers })
      if (!res.ok) {
        lastErr = new Error(`HTTP ${res.status}`)
        continue
      }
      const data: unknown = await res.json()
      if (isOpenApiDocument(data)) {
        return data
      }
    } catch (e) {
      lastErr = e
    }
  }
  throw lastErr ?? new Error('Could not load OpenAPI specification')
}
