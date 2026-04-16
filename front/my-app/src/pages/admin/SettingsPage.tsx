/**
 * Admin > Settings — platform display name, default currency, maintenance flag (GET/PUT /admin/settings).
 */

import { useEffect, useState } from 'react'
import { useDisplayCurrency } from '../../context/DisplayCurrencyContext'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, settingsAPI } from '../../services/api'
import type { SystemSettingsDto } from '../../types/api'
import { Card } from '../../components/Card'

export function SettingsPage() {
  const { t } = useLanguage()
  const { refreshDisplayCurrency } = useDisplayCurrency()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  const [companyDisplayName, setCompanyDisplayName] = useState('')
  const [defaultCurrency, setDefaultCurrency] = useState('USD')
  const [maintenanceMode, setMaintenanceMode] = useState(false)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    settingsAPI
      .get()
      .then((res) => {
        const d = res.data as SystemSettingsDto
        if (!cancelled) {
          setCompanyDisplayName(d.companyDisplayName ?? '')
          setDefaultCurrency((d.defaultCurrency ?? 'USD').toUpperCase())
          setMaintenanceMode(Boolean(d.maintenanceMode))
        }
      })
      .catch((e) => {
        if (!cancelled) setError(getApiErrorMessage(e))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    setSaved(false)
    const cur = defaultCurrency.trim().toUpperCase()
    if (cur.length !== 3 || !/^[A-Z]{3}$/.test(cur)) {
      setError(t('settingsPage.currencyInvalid'))
      setSaving(false)
      return
    }
    try {
      const updated = await settingsAPI.update({
        companyDisplayName: companyDisplayName.trim(),
        defaultCurrency: cur,
        maintenanceMode,
      })
      const d = updated.data as SystemSettingsDto
      setCompanyDisplayName(d.companyDisplayName ?? '')
      setDefaultCurrency((d.defaultCurrency ?? 'USD').toUpperCase())
      setMaintenanceMode(Boolean(d.maintenanceMode))
      setSaved(true)
      refreshDisplayCurrency()
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('settingsPage.title')}</h1>

      {error && (
        <p className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
          {error}
        </p>
      )}
      {saved && (
        <p className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/40 dark:text-emerald-200">
          {t('settingsPage.saved')}
        </p>
      )}

      {loading ? (
        <p className="mt-6 text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
      ) : (
        <Card className="mt-6 max-w-lg p-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
                {t('settingsPage.fieldCompanyName')}
              </label>
              <input
                value={companyDisplayName}
                onChange={(ev) => setCompanyDisplayName(ev.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
                {t('settingsPage.fieldDefaultCurrency')}
              </label>
              <input
                value={defaultCurrency}
                onChange={(ev) => setDefaultCurrency(ev.target.value.toUpperCase())}
                maxLength={3}
                className="mt-1 w-28 rounded-lg border border-slate-300 bg-white px-3 py-2 font-mono uppercase dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
              />
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{t('settingsPage.currencyHint')}</p>
            </div>
            <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-700 dark:text-slate-200">
              <input
                type="checkbox"
                checked={maintenanceMode}
                onChange={(ev) => setMaintenanceMode(ev.target.checked)}
                className="h-4 w-4 rounded border-slate-300"
              />
              {t('settingsPage.fieldMaintenance')}
            </label>
            <button
              type="submit"
              disabled={saving}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {saving ? t('settingsPage.saving') : t('settingsPage.save')}
            </button>
          </form>
        </Card>
      )}
    </>
  )
}
