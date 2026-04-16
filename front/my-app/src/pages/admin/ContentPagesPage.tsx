import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { contentPagesAPI, getApiErrorMessage } from '../../services/api'
import { useLanguage } from '../../context/LanguageContext'
import { useAuth } from '../../context/AuthContext'

type Slug = 'home' | 'about' | 'services' | 'contact' | 'faq' | 'privacy' | 'terms'

const SLUGS: Slug[] = ['home', 'about', 'services', 'contact', 'faq', 'privacy', 'terms']

function safeParseJson(text: string): Record<string, unknown> | null {
  try {
    const parsed = JSON.parse(text) as unknown
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      return parsed as Record<string, unknown>
    }
    return null
  } catch {
    return null
  }
}

export function ContentPagesPage() {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [slug, setSlug] = useState<Slug>('home')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [published, setPublished] = useState(true)
  const [enJson, setEnJson] = useState('{\n  \n}')
  const [arJson, setArJson] = useState('{\n  \n}')
  const [statusMessage, setStatusMessage] = useState('')
  const [error, setError] = useState('')

  const slugLabel = useMemo(() => slug.toUpperCase(), [slug])

  if (!user) return null
  const canEditContent = user.role === 'super_admin' || user.role === 'admin'
  if (!canEditContent) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-800 dark:bg-amber-900/20">
        <h2 className="text-xl font-semibold text-amber-800 dark:text-amber-200">{t('access.restrictedTitle')}</h2>
        <p className="mt-2 text-amber-700 dark:text-amber-300">Only super admin and sales/admin roles can edit landing content. You are signed in as {user.role}.</p>
        <button
          type="button"
          onClick={() => navigate('/dashboard')}
          className="mt-4 rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700"
        >
          {t('access.backToDashboard')}
        </button>
      </div>
    )
  }

  useEffect(() => {
    setLoading(true)
    setError('')
    setStatusMessage('')
    Promise.all([contentPagesAPI.get(slug, 'en'), contentPagesAPI.get(slug, 'ar')])
      .then(([enRes, arRes]) => {
        const enData = (enRes.data?.content ?? {}) as Record<string, unknown>
        const arData = (arRes.data?.content ?? {}) as Record<string, unknown>
        setPublished(Boolean(enRes.data?.published ?? arRes.data?.published ?? true))
        setEnJson(JSON.stringify(enData, null, 2))
        setArJson(JSON.stringify(arData, null, 2))
      })
      .catch((err) => {
        setError(getApiErrorMessage(err, 'Failed to load content page'))
      })
      .finally(() => setLoading(false))
  }, [slug])

  const handleSave = async () => {
    setSaving(true)
    setError('')
    setStatusMessage('')
    const enObject = safeParseJson(enJson)
    const arObject = safeParseJson(arJson)
    if (!enObject || !arObject) {
      setSaving(false)
      setError('EN/AR fields must be valid JSON objects.')
      return
    }
    try {
      await contentPagesAPI.upsert(slug, {
        contentEn: enObject,
        contentAr: arObject,
        published,
      })
      setStatusMessage(`Saved ${slug} successfully.`)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Failed to save content page'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">{t('title.content')}</h1>
      </div>

      <div className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-900/70 lg:grid-cols-[220px_1fr_1fr]">
        <div className="space-y-2">
          <label className="text-xs font-semibold uppercase tracking-[0.1em] text-slate-500 dark:text-slate-400">Page</label>
          <select
            value={slug}
            onChange={(e) => setSlug(e.target.value as Slug)}
            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            {SLUGS.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
          <label className="mt-3 inline-flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
            <input
              type="checkbox"
              checked={published}
              onChange={(e) => setPublished(e.target.checked)}
              className="rounded border-slate-300 text-primary"
            />
            Published
          </label>
          <button
            type="button"
            onClick={handleSave}
            disabled={saving || loading}
            className="dashboard-btn-primary mt-3 w-full disabled:opacity-60"
          >
            {saving ? 'Saving...' : `Save ${slugLabel}`}
          </button>
          {statusMessage ? <p className="text-xs text-emerald-700 dark:text-emerald-400">{statusMessage}</p> : null}
          {error ? <p className="text-xs text-red-700 dark:text-red-400">{error}</p> : null}
        </div>

        <div className="space-y-2">
          <label className="text-xs font-semibold uppercase tracking-[0.1em] text-slate-500 dark:text-slate-400">English JSON</label>
          <textarea
            value={enJson}
            onChange={(e) => setEnJson(e.target.value)}
            className="min-h-[26rem] w-full rounded-xl border border-slate-300 bg-white p-3 font-mono text-xs text-slate-900 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            spellCheck={false}
          />
        </div>

        <div className="space-y-2">
          <label className="text-xs font-semibold uppercase tracking-[0.1em] text-slate-500 dark:text-slate-400">Arabic JSON</label>
          <textarea
            value={arJson}
            onChange={(e) => setArJson(e.target.value)}
            className="min-h-[26rem] w-full rounded-xl border border-slate-300 bg-white p-3 font-mono text-xs text-slate-900 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            spellCheck={false}
          />
        </div>
      </div>
    </section>
  )
}
