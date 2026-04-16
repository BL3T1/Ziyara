import { useCallback, useEffect, useMemo, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { fetchOpenApiSpecification } from '../../services/openapi'
import { getApiErrorMessage } from '../../services/api'
import type { OpenAPIOperation, OpenAPIParameter, OpenAPISpec } from '../../types/openapi'

const HTTP_METHODS = ['get', 'post', 'put', 'patch', 'delete', 'options', 'head', 'trace'] as const

type HttpMethodLower = (typeof HTTP_METHODS)[number]

type OperationEntry = {
  path: string
  method: HttpMethodLower
  operation: OpenAPIOperation
  pathLevelParams: OpenAPIParameter[]
}

function methodBadgeClass(method: string): string {
  const m = method.toUpperCase()
  const base = 'rounded-md px-2 py-0.5 text-xs font-bold uppercase tracking-wide'
  switch (m) {
    case 'GET':
      return `${base} bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-200`
    case 'POST':
      return `${base} bg-sky-100 text-sky-800 dark:bg-sky-900/40 dark:text-sky-200`
    case 'PUT':
      return `${base} bg-amber-100 text-amber-900 dark:bg-amber-900/40 dark:text-amber-200`
    case 'PATCH':
      return `${base} bg-orange-100 text-orange-900 dark:bg-orange-900/40 dark:text-orange-200`
    case 'DELETE':
      return `${base} bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-200`
    default:
      return `${base} bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200`
  }
}

function schemaToShort(schema: unknown, depth = 0): string {
  if (depth > 5) return '…'
  if (schema == null) return ''
  if (typeof schema !== 'object') return String(schema)
  const s = schema as Record<string, unknown>
  if (typeof s.$ref === 'string') {
    const parts = s.$ref.split('/')
    return parts[parts.length - 1] ?? s.$ref
  }
  if (s.type === 'array') {
    return `${schemaToShort(s.items, depth + 1)}[]`
  }
  if (s.type === 'object' && s.properties && typeof s.properties === 'object') {
    const keys = Object.keys(s.properties as object)
    const head = keys.slice(0, 6).join(', ')
    return `{ ${head}${keys.length > 6 ? ', …' : ''} }`
  }
  if (typeof s.type === 'string') {
    const fmt = typeof s.format === 'string' ? ` · ${s.format}` : ''
    const en = Array.isArray(s.enum) ? ` (${(s.enum as unknown[]).slice(0, 3).join('|')}${(s.enum as unknown[]).length > 3 ? '…' : ''})` : ''
    return `${s.type}${fmt}${en}`
  }
  if (s.oneOf || s.anyOf || s.allOf) {
    return 'union'
  }
  return 'object'
}

function mergeParams(pathLevel: OpenAPIParameter[], opLevel: OpenAPIParameter[] | undefined): OpenAPIParameter[] {
  const byKey = new Map<string, OpenAPIParameter>()
  for (const p of pathLevel) {
    const k = `${p.in ?? ''}:${p.name ?? ''}`
    if (p.name) byKey.set(k, p)
  }
  for (const p of opLevel ?? []) {
    const k = `${p.in ?? ''}:${p.name ?? ''}`
    if (p.name) byKey.set(k, p)
  }
  return [...byKey.values()]
}

function flattenOperations(spec: OpenAPISpec): OperationEntry[] {
  const paths = spec.paths ?? {}
  const out: OperationEntry[] = []
  for (const [path, pathItem] of Object.entries(paths)) {
    if (!pathItem || typeof pathItem !== 'object') continue
    const pathLevelParams = Array.isArray(pathItem.parameters) ? pathItem.parameters : []
    for (const method of HTTP_METHODS) {
      const raw = pathItem[method]
      if (!raw || typeof raw !== 'object' || Array.isArray(raw)) continue
      const operation = raw as OpenAPIOperation
      out.push({
        path,
        method,
        operation,
        pathLevelParams,
      })
    }
  }
  return out
}

function groupByTag(entries: OperationEntry[], otherTag: string): Map<string, OperationEntry[]> {
  const map = new Map<string, OperationEntry[]>()
  for (const e of entries) {
    const tags = e.operation.tags?.length ? e.operation.tags : [otherTag]
    for (const tag of tags) {
      const list = map.get(tag) ?? []
      list.push(e)
      map.set(tag, list)
    }
  }
  for (const list of map.values()) {
    list.sort((a, b) => a.path.localeCompare(b.path) || a.method.localeCompare(b.method))
  }
  return map
}

function filterEntries(entries: OperationEntry[], query: string): OperationEntry[] {
  const q = query.trim().toLowerCase()
  if (!q) return entries
  return entries.filter((e) => {
    const hay = [
      e.path,
      e.method,
      e.operation.summary,
      e.operation.description,
      e.operation.operationId,
      ...(e.operation.tags ?? []),
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return hay.includes(q)
  })
}

function downloadJson(spec: OpenAPISpec) {
  const blob = new Blob([JSON.stringify(spec, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'openapi.json'
  a.click()
  URL.revokeObjectURL(url)
}

export function OpenApiDocView() {
  const { t } = useLanguage()
  const [spec, setSpec] = useState<OpenAPISpec | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [showRaw, setShowRaw] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    setError('')
    fetchOpenApiSpecification()
      .then(setSpec)
      .catch((e) => setError(getApiErrorMessage(e, t('openApi.failedLoadSpec'))))
      .finally(() => setLoading(false))
  }, [t])

  useEffect(() => {
    load()
  }, [load])

  const allEntries = useMemo(() => (spec ? flattenOperations(spec) : []), [spec])
  const filtered = useMemo(() => filterEntries(allEntries, query), [allEntries, query])
  const grouped = useMemo(() => groupByTag(filtered, t('openApi.tagOther')), [filtered, t])

  const tagOrder = useMemo(() => {
    const fromSpec = spec?.tags?.map((t) => t.name) ?? []
    const keys = [...grouped.keys()]
    const ordered: string[] = []
    for (const t of fromSpec) {
      if (keys.includes(t) && !ordered.includes(t)) ordered.push(t)
    }
    for (const t of keys.sort()) {
      if (!ordered.includes(t)) ordered.push(t)
    }
    return ordered
  }, [spec, grouped])

  const swaggerUiHref = useMemo(() => {
    const raw = import.meta.env.VITE_API_URL ?? '/api/v1'
    const trimmed = raw.replace(/\/$/, '')
    if (trimmed.startsWith('/')) return `${trimmed}/swagger-ui.html`
    try {
      const api = new URL(trimmed)
      return `${api.origin}${api.pathname.replace(/\/$/, '')}/swagger-ui.html`
    } catch {
      return '/api/v1/swagger-ui.html'
    }
  }, [])

  if (loading) {
    return (
      <div className="flex min-h-[320px] items-center justify-center rounded-xl border border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-800/50">
        <p className="text-slate-600 dark:text-slate-400">{t('ui.openApiLoading')}</p>
      </div>
    )
  }

  if (error || !spec) {
    return (
      <div className="rounded-xl border border-rose-200 bg-rose-50 p-6 dark:border-rose-900 dark:bg-rose-950/30">
        <p className="font-medium text-rose-800 dark:text-rose-200">{error || t('openApi.noSpecLoaded')}</p>
        <button
          type="button"
          onClick={load}
          className="mt-4 rounded-lg bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-700"
        >
          {t('ui.retry')}
        </button>
      </div>
    )
  }

  const info = spec.info

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">{info?.title ?? 'API'}</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            OpenAPI {spec.openapi ?? '3.x'}
            {info?.version ? ` · v${info.version}` : ''}
          </p>
          {spec.servers && spec.servers.length > 0 && (
            <ul className="mt-3 space-y-1 text-sm text-slate-600 dark:text-slate-400">
              {spec.servers.map((s, i) => (
                <li key={i}>
                  <span className="font-mono text-indigo-600 dark:text-indigo-400">{s.url}</span>
                  {s.description ? ` — ${s.description}` : ''}
                </li>
              ))}
            </ul>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => downloadJson(spec)}
            className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-800 shadow-sm hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:hover:bg-slate-700"
          >
            {t('openApi.downloadOpenapiJson')}
          </button>
          <a
            href={swaggerUiHref}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center rounded-xl border border-indigo-200 bg-indigo-50 px-4 py-2 text-sm font-semibold text-indigo-800 hover:bg-indigo-100 dark:border-indigo-800 dark:bg-indigo-950/50 dark:text-indigo-200 dark:hover:bg-indigo-900/40"
          >
            {t('openApi.swaggerUiNewTab')}
          </a>
        </div>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <label className="block flex-1">
          <span className="sr-only">{t('openApi.searchPlaceholder')}</span>
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={t('openApi.searchPlaceholder')}
            className="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm text-slate-900 shadow-sm placeholder:text-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder:text-slate-500"
          />
        </label>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          {t('openApi.operationsCount', { shown: filtered.length, total: allEntries.length })}
        </p>
      </div>

      <div className="space-y-10">
        {tagOrder.map((tag) => {
          const ops = grouped.get(tag)
          if (!ops?.length) return null
          const tagDesc = spec.tags?.find((t) => t.name === tag)?.description
          return (
            <section key={tag}>
              <div className="mb-4 border-b border-slate-200 pb-2 dark:border-slate-700">
                <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{tag}</h2>
                {tagDesc && <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{tagDesc}</p>}
              </div>
              <div className="space-y-3">
                {ops.map((e, idx) => (
                  <OperationCard key={`${e.method}-${e.path}-${idx}`} entry={e} t={t} />
                ))}
              </div>
            </section>
          )
        })}
      </div>

      <div className="rounded-xl border border-slate-200 dark:border-slate-700">
        <button
          type="button"
          onClick={() => setShowRaw((v) => !v)}
          className="flex w-full items-center justify-between px-4 py-3 text-left text-sm font-semibold text-slate-800 hover:bg-slate-50 dark:text-slate-200 dark:hover:bg-slate-800/80"
        >
          {t('openApi.rawOpenApiJson')}
          <span className="text-slate-400">{showRaw ? '▼' : '▶'}</span>
        </button>
        {showRaw && (
          <pre className="max-h-[min(480px,50vh)] overflow-auto border-t border-slate-200 bg-slate-50 p-4 text-xs text-slate-800 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200">
            {JSON.stringify(spec, null, 2)}
          </pre>
        )}
      </div>
    </div>
  )
}

function OperationCard({
  entry,
  t,
}: {
  entry: OperationEntry
  t: (key: string, params?: Record<string, string | number>) => string
}) {
  const { path, method, operation, pathLevelParams } = entry
  const params = mergeParams(pathLevelParams, operation.parameters)
  const summary = operation.summary ?? operation.operationId ?? '—'
  const body = operation.requestBody
  const responses = operation.responses ?? {}

  return (
    <details className="group rounded-xl border border-slate-200 bg-white shadow-sm open:shadow-md dark:border-slate-700 dark:bg-slate-800/40">
      <summary className="flex cursor-pointer list-none flex-wrap items-center gap-3 rounded-xl px-4 py-3 marker:hidden [&::-webkit-details-marker]:hidden">
        <span className={methodBadgeClass(method)}>{method.toUpperCase()}</span>
        <code className="break-all text-sm font-medium text-slate-800 dark:text-slate-200">{path}</code>
        <span className="min-w-0 flex-1 text-left text-sm text-slate-600 dark:text-slate-300">{summary}</span>
        <span className="text-slate-400 transition group-open:rotate-180">▼</span>
      </summary>
      <div className="space-y-4 border-t border-slate-100 px-4 py-4 dark:border-slate-700">
        {operation.description && (
          <p className="text-sm leading-relaxed text-slate-600 dark:text-slate-300">{operation.description}</p>
        )}
        {operation.operationId && (
          <p className="font-mono text-xs text-slate-500 dark:text-slate-400">operationId: {operation.operationId}</p>
        )}

        {params.length > 0 && (
          <div>
            <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
              {t('openApi.parameters')}
            </h3>
            <div className="table-shell rounded-xl">
              <table className="data-table w-full min-w-[480px] text-left text-sm">
                <thead>
                  <tr>
                    <th className="px-3 py-2.5">{t('openApi.paramIn')}</th>
                    <th className="px-3 py-2.5">{t('openApi.paramName')}</th>
                    <th className="px-3 py-2.5">{t('openApi.paramType')}</th>
                    <th className="px-3 py-2.5">{t('openApi.paramReq')}</th>
                    <th className="px-3 py-2.5">{t('openApi.paramDescription')}</th>
                  </tr>
                </thead>
                <tbody>
                  {params.map((p, i) => (
                    <tr key={`${p.name}-${i}`}>
                      <td className="px-3 py-2 text-slate-600 dark:text-slate-400">{p.in ?? '—'}</td>
                      <td className="px-3 py-2 font-mono text-xs text-slate-900 dark:text-slate-100">{p.name}</td>
                      <td className="px-3 py-2 font-mono text-xs text-slate-600 dark:text-slate-300">
                        {schemaToShort(p.schema)}
                      </td>
                      <td className="px-3 py-2 text-slate-600 dark:text-slate-400">{p.required ? t('openApi.yes') : t('openApi.no')}</td>
                      <td className="px-3 py-2 text-slate-600 dark:text-slate-300">{p.description ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {body?.content && Object.keys(body.content).length > 0 && (
          <div>
            <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
              {t('openApi.requestBody')}
              {body.required ? t('openApi.requiredSuffix') : ''}
            </h3>
            {body.description && <p className="mb-2 text-sm text-slate-600 dark:text-slate-300">{body.description}</p>}
            <ul className="space-y-2 text-sm">
              {Object.entries(body.content).map(([mime, meta]) => (
                <li key={mime} className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 dark:border-slate-600 dark:bg-slate-900/40">
                  <span className="font-mono text-xs text-indigo-600 dark:text-indigo-400">{mime}</span>
                  <p className="mt-1 font-mono text-xs text-slate-700 dark:text-slate-300">{schemaToShort(meta?.schema)}</p>
                </li>
              ))}
            </ul>
          </div>
        )}

        {Object.keys(responses).length > 0 && (
          <div>
            <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
              {t('openApi.responses')}
            </h3>
            <ul className="space-y-2">
              {Object.entries(responses)
                .sort(([a], [b]) => a.localeCompare(b))
                .map(([code, res]) => (
                  <li
                    key={code}
                    className="flex flex-wrap items-baseline gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-600"
                  >
                    <span className="font-mono font-semibold text-slate-900 dark:text-slate-100">{code}</span>
                    <span className="text-slate-600 dark:text-slate-300">{res.description ?? ''}</span>
                    {res.content &&
                      Object.entries(res.content).map(([mime, c]) => (
                        <span key={mime} className="font-mono text-xs text-slate-500 dark:text-slate-400">
                          {mime}: {schemaToShort(c?.schema)}
                        </span>
                      ))}
                  </li>
                ))}
            </ul>
          </div>
        )}
      </div>
    </details>
  )
}
