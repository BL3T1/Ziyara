import type { OpenAPISpec } from '../types/openapi'
import apiClient from './api'

function isOpenApiDocument(data: unknown): data is OpenAPISpec {
  if (!data || typeof data !== 'object') return false
  const o = data as Record<string, unknown>
  return typeof o.openapi === 'string' && o.openapi.startsWith('3.')
}

/**
 * Load raw OpenAPI JSON from Springdoc (configured path + default).
 * Uses the same axios instance as the app (base URL, JWT, 401 handling).
 */
export async function fetchOpenApiSpecification(): Promise<OpenAPISpec> {
  const candidates = ['/api-docs', '/v3/api-docs']
  let lastErr: unknown
  for (const path of candidates) {
    try {
      const res = await apiClient.get(path)
      if (isOpenApiDocument(res.data)) {
        return res.data
      }
    } catch (e) {
      lastErr = e
    }
  }
  throw lastErr ?? new Error('Could not load OpenAPI specification')
}
