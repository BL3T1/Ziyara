/**
 * Subset of OpenAPI 3.x used by the in-app API docs viewer.
 */

export type OpenAPISpec = {
  openapi?: string
  info?: {
    title?: string
    version?: string
    description?: string
  }
  servers?: { url?: string; description?: string }[]
  tags?: { name: string; description?: string }[]
  paths?: Record<string, OpenAPIPathItem>
  components?: {
    schemas?: Record<string, unknown>
    securitySchemes?: Record<string, unknown>
  }
}

export type OpenAPIPathItem = {
  parameters?: OpenAPIParameter[]
} & Record<string, OpenAPIOperation | OpenAPIParameter[] | undefined>

export type OpenAPIParameter = {
  name?: string
  in?: string
  required?: boolean
  description?: string
  schema?: Record<string, unknown>
  $ref?: string
}

export type OpenAPIOperation = {
  tags?: string[]
  summary?: string
  description?: string
  operationId?: string
  parameters?: OpenAPIParameter[]
  requestBody?: {
    description?: string
    required?: boolean
    content?: Record<string, { schema?: unknown }>
  }
  responses?: Record<string, { description?: string; content?: Record<string, { schema?: unknown }> }>
  security?: Record<string, string[]>[]
}
