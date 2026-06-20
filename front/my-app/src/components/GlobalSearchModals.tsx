/**
 * Phase 6: global command search + super-admin deleted archive search (header modals).
 */

import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useLanguage } from '../context/LanguageContext'
import {
  adminSuperAPI,
  bookingsAPI,
  getApiErrorMessage,
  providersAPI,
  servicesAPI,
  usersAPI,
} from '../services/api'
import type { BookingDto, DeletedItemDto, PageDto, ServiceDto, UserDto } from '../types/api'
import { isSuperAdminRole } from '../types/auth'
import { isUuid } from '../utils/isUuid'
import { serviceTypeToListingsPath } from '../utils/serviceTypeSegment'

function asPage<T>(data: unknown): PageDto<T> | null {
  if (data && typeof data === 'object' && Array.isArray((data as PageDto<T>).content)) {
    return data as PageDto<T>
  }
  return null
}

type SearchHit =
  | { kind: 'booking'; id: string; title: string; subtitle?: string }
  | { kind: 'provider'; id: string; title: string; subtitle?: string }
  | { kind: 'service'; id: string; title: string; subtitle?: string; serviceType?: string }
  | { kind: 'user'; id: string; title: string; subtitle?: string }
  | { kind: 'customer'; id: string; title: string; subtitle?: string }

/** Human-readable fallbacks when API rows lack names/refs (never show raw UUIDs). */
type SearchTitleFallbacks = {
  booking: string
  provider: string
  service: string
  user: string
  customer: string
}

async function runGlobalSearch(
  raw: string,
  isSuperAdmin: boolean,
  fallbacks: SearchTitleFallbacks,
  onHit: (h: SearchHit) => void,
): Promise<void> {
  const q = raw.trim()
  if (q.length < 2 && !isUuid(q)) return

  const seen = new Set<string>()
  const add = (h: SearchHit) => {
    const k = `${h.kind}:${h.id}`
    if (seen.has(k)) return
    seen.add(k)
    onHit(h)
  }

  if (isUuid(q)) {
    await Promise.allSettled([
      bookingsAPI.get(q).then((r) => {
        const b = r.data as BookingDto
        if (b?.id)
          add({
            kind: 'booking',
            id: b.id,
            title: b.bookingReference?.trim() || fallbacks.booking,
            subtitle: b.status,
          })
      }),
      providersAPI.get(q).then((r) => {
        const p = r.data as { id?: string; name?: string; status?: string }
        if (p?.id)
          add({
            kind: 'provider',
            id: p.id,
            title: (typeof p.name === 'string' && p.name.trim()) || fallbacks.provider,
            subtitle: p.status,
          })
      }),
      usersAPI.get(q).then((r) => {
        const u = r.data as UserDto
        if (u?.id)
          add({
            kind: 'user',
            id: u.id,
            title: u.fullName?.trim() || u.email?.trim() || fallbacks.user,
            subtitle: u.email,
          })
      }),
    ])
  }

  if (q.length >= 3) {
    try {
      const r = await bookingsAPI.getByReference(q)
      const b = r.data as BookingDto
      if (b?.id)
        add({
          kind: 'booking',
          id: b.id,
          title: b.bookingReference?.trim() || q.trim() || fallbacks.booking,
          subtitle: b.status,
        })
    } catch {
      /* not found */
    }
  }

  if (q.length >= 2) {
    try {
      const r = await servicesAPI.search({ q, size: 20 })
      const page = asPage<ServiceDto>(r.data)
      for (const s of page?.content ?? []) {
        if (s?.id)
          add({
            kind: 'service',
            id: s.id,
            title: s.name?.trim() || fallbacks.service,
            subtitle: s.type,
            serviceType: s.type,
          })
      }
    } catch {
      /* ignore */
    }
  }

  if (isSuperAdmin && q.length >= 2) {
    try {
      const r = await adminSuperAPI.searchCustomers(q, 15)
      const arr = Array.isArray(r.data) ? (r.data as UserDto[]) : []
      for (const u of arr) {
        if (u?.id)
          add({
            kind: 'customer',
            id: u.id,
            title: u.fullName?.trim() || u.email?.trim() || fallbacks.customer,
            subtitle: u.email,
          })
      }
    } catch {
      /* ignore */
    }
  }
}

export function GlobalSearchModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { t } = useLanguage()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [hits, setHits] = useState<SearchHit[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const isSuperAdmin = isSuperAdminRole(user?.role)

  useEffect(() => {
    if (!open) return
    setQuery('')
    setHits([])
    setError(null)
    const t0 = requestAnimationFrame(() => inputRef.current?.focus())
    return () => cancelAnimationFrame(t0)
  }, [open])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  useEffect(() => {
    if (!open) return
    const q = query.trim()
    if (q.length < 2 && !isUuid(q)) {
      setHits([])
      setLoading(false)
      return
    }
    let cancelled = false
    setLoading(true)
    setError(null)
    const timer = window.setTimeout(() => {
      const acc: SearchHit[] = []
      const fallbacks: SearchTitleFallbacks = {
        booking: t('globalSearch.kind.booking'),
        provider: t('globalSearch.kind.provider'),
        service: t('globalSearch.kind.service'),
        user: t('globalSearch.kind.user'),
        customer: t('globalSearch.kind.customer'),
      }
      runGlobalSearch(q, isSuperAdmin, fallbacks, (h) => acc.push(h))
        .then(() => {
          if (!cancelled) setHits(acc)
        })
        .catch((e) => {
          if (!cancelled) setError(getApiErrorMessage(e))
        })
        .finally(() => {
          if (!cancelled) setLoading(false)
        })
    }, 320)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [query, open, isSuperAdmin, t])

  const onSelect = useCallback(
    (h: SearchHit) => {
      onClose()
      switch (h.kind) {
        case 'booking':
          navigate(`/management/bookings?bookingId=${encodeURIComponent(h.id)}`)
          break
        case 'provider':
          navigate(`/management/providers/${encodeURIComponent(h.id)}`)
          break
        case 'service':
          navigate(`/services/${serviceTypeToListingsPath(h.serviceType)}/${h.id}`)
          break
        case 'user':
          navigate('/management/users')
          break
        case 'customer':
          navigate(`/admin/customers/${encodeURIComponent(h.id)}`)
          break
        default:
          break
      }
    },
    [navigate, onClose],
  )

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-[60] flex items-start justify-center p-4 pt-[12vh] [animation:modal-overlay-in_180ms_ease_both]"
      style={{ background: 'rgb(2 4 9 / 0.65)', backdropFilter: 'blur(3px)', WebkitBackdropFilter: 'blur(3px)' }}
      role="dialog"
      aria-modal="true"
      aria-label={t('globalSearch.title')}
      onClick={onClose}
    >
      <div
        className="w-full max-w-lg overflow-hidden rounded-2xl border border-slate-200/60 bg-white shadow-2xl [animation:modal-panel-in_240ms_cubic-bezier(0.34,1.2,0.64,1)_both] dark:border-white/[0.08] dark:bg-slate-900"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-700">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
            {t('globalSearch.hintKeys')}
          </p>
          <input
            ref={inputRef}
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={t('globalSearch.placeholder')}
            className="mt-2 w-full border-0 bg-transparent text-base text-slate-900 outline-none placeholder:text-slate-400 dark:text-slate-100"
          />
        </div>
        <div className="max-h-[min(50vh,24rem)] overflow-y-auto p-2">
          {error && (
            <p className="px-2 py-2 text-sm text-red-600 dark:text-red-400">{error}</p>
          )}
          {loading && (
            <p className="px-2 py-4 text-center text-sm text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
          )}
          {!loading && hits.length === 0 && query.trim().length >= 2 && (
            <p className="px-2 py-4 text-center text-sm text-slate-500 dark:text-slate-400">{t('globalSearch.noResults')}</p>
          )}
          <ul className="space-y-1">
            {hits.map((h) => (
              <li key={`${h.kind}:${h.id}`}>
                <button
                  type="button"
                  onClick={() => onSelect(h)}
                  className="flex w-full flex-col rounded-xl px-3 py-2.5 text-left text-sm transition-colors hover:bg-slate-100 dark:hover:bg-slate-800"
                >
                  <span className="font-medium text-slate-900 dark:text-slate-100">{h.title}</span>
                  <span className="text-xs text-slate-500 dark:text-slate-400">
                    {t(`globalSearch.kind.${h.kind}`)}
                    {h.subtitle ? ` · ${h.subtitle}` : ''}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}

export function DeletedArchiveSearchModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const [q, setQ] = useState('')
  const [rows, setRows] = useState<DeletedItemDto[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [restoring, setRestoring] = useState<string | null>(null)
  const [listSource, setListSource] = useState<'recent' | 'search'>('recent')

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  useEffect(() => {
    if (!open) return
    setListSource('recent')
    setError(null)
    setInfo(null)
    setLoading(true)
    adminSuperAPI
      .listRecentDeleted(30)
      .then((res) => {
        const data = res.data
        setRows(Array.isArray(data) ? (data as DeletedItemDto[]) : [])
      })
      .catch((e) => {
        setError(getApiErrorMessage(e))
        setRows([])
      })
      .finally(() => setLoading(false))
  }, [open])

  const search = () => {
    const term = q.trim()
    if (!term) {
      setError(t('deletedItemsPage.enterQuery'))
      return
    }
    if (isUuid(term)) {
      setError(t('deletedItemsPage.noUuidSearch'))
      return
    }
    setError(null)
    setInfo(null)
    setLoading(true)
    setListSource('search')
    adminSuperAPI
      .searchDeleted(term)
      .then((res) => {
        const data = res.data
        setRows(Array.isArray(data) ? (data as DeletedItemDto[]) : [])
      })
      .catch((e) => {
        setError(getApiErrorMessage(e))
        setRows([])
      })
      .finally(() => setLoading(false))
  }

  const restore = (row: DeletedItemDto) => {
    const key = `${row.entityType}:${row.id}`
    setRestoring(key)
    setError(null)
    setInfo(null)
    adminSuperAPI
      .restoreDeleted({ entityType: row.entityType, id: row.id })
      .then(() => {
        setInfo(t('deletedItemsPage.restored', { entityType: row.entityType, label: row.label ?? '' }))
        setRows((prev) => prev.filter((r) => r.id !== row.id))
      })
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setRestoring(null))
  }

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-[60] flex items-start justify-center p-4 pt-[12vh] [animation:modal-overlay-in_180ms_ease_both]"
      style={{ background: 'rgb(2 4 9 / 0.65)', backdropFilter: 'blur(3px)', WebkitBackdropFilter: 'blur(3px)' }}
      role="dialog"
      aria-modal="true"
      aria-label={t('globalSearch.deletedTitle')}
      onClick={onClose}
    >
      <div
        className="w-full max-w-lg overflow-hidden rounded-2xl border border-slate-200/60 bg-white p-5 shadow-2xl [animation:modal-panel-in_240ms_cubic-bezier(0.34,1.2,0.64,1)_both] dark:border-white/[0.08] dark:bg-slate-900"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('globalSearch.deletedTitle')}</h2>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{t('globalSearch.deletedIntro')}</p>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{t('deletedItemsPage.recentAutoLoad')}</p>
        <div className="mt-4 flex gap-2">
          <input
            type="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && search()}
            placeholder={t('deletedItemsPage.placeholder')}
            className="min-w-0 flex-1 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          />
          <button
            type="button"
            onClick={search}
            disabled={loading}
            className="dashboard-btn-primary shrink-0 px-3 py-2 text-sm"
          >
            {loading && listSource === 'search'
              ? t('ui.searching')
              : loading
                ? t('ui.loading')
                : t('ui.searchAction')}
          </button>
        </div>
        {error && <p className="mt-2 text-sm text-red-600 dark:text-red-400">{error}</p>}
        {info && <p className="mt-2 text-sm text-green-700 dark:text-green-300">{info}</p>}
        <ul className="mt-4 max-h-60 space-y-2 overflow-y-auto">
          {rows.length === 0 && !loading ? (
            <li className="px-1 py-2 text-center text-sm text-slate-500 dark:text-slate-400">
              {listSource === 'search' ? t('deletedItemsPage.noSearchResults') : t('deletedItemsPage.noRecent')}
            </li>
          ) : null}
          {rows.map((row) => (
            <li
              key={`${row.entityType}:${row.id}`}
              className="flex items-center justify-between gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-600"
            >
              <span className="min-w-0 truncate text-slate-800 dark:text-slate-100">
                <span className="font-mono text-xs text-slate-500">{row.entityType}</span> · {row.label?.trim() || t('ui.emDash')}
              </span>
              <button
                type="button"
                disabled={restoring === `${row.entityType}:${row.id}`}
                onClick={() => restore(row)}
                className="shrink-0 text-primary hover:underline disabled:opacity-50"
              >
                {restoring === `${row.entityType}:${row.id}` ? t('ui.restoring') : t('ui.restore')}
              </button>
            </li>
          ))}
        </ul>
        <button
          type="button"
          onClick={() => {
            onClose()
            navigate('/admin/deleted-items')
          }}
          className="mt-4 text-sm font-medium text-primary hover:underline"
        >
          {t('globalSearch.openFullDeletedPage')}
        </button>
      </div>
    </div>
  )
}
