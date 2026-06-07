/**
 * Admin > Integrations — feature flags (exec) + integration API keys (super admin only).
 */

import { useCallback, useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { usePermission } from '../../hooks/usePermission'
import { getApiErrorMessage, integrationsAPI } from '../../services/api'
import { Card } from '../../components/Card'
import { Modal } from '../../components/Modal'
import type { FeatureFlagDto, IntegrationApiKeyCreatedDto, IntegrationApiKeySummaryDto } from '../../types/api'

export function IntegrationsPage() {
  const { t } = useLanguage()
  const canManageKeys = usePermission('settings:write')

  const [error, setError] = useState<string | null>(null)
  const [flags, setFlags] = useState<FeatureFlagDto[]>([])
  const [flagsLoading, setFlagsLoading] = useState(true)
  const [flagKey, setFlagKey] = useState('')
  const [flagDesc, setFlagDesc] = useState('')
  const [flagEnabled, setFlagEnabled] = useState(false)
  const [flagSaving, setFlagSaving] = useState(false)

  const [keys, setKeys] = useState<IntegrationApiKeySummaryDto[]>([])
  const [keysLoading, setKeysLoading] = useState(false)
  const [keyName, setKeyName] = useState('')
  const [keyCreating, setKeyCreating] = useState(false)
  const [createdSecret, setCreatedSecret] = useState<IntegrationApiKeyCreatedDto | null>(null)
  const [revokingId, setRevokingId] = useState<string | null>(null)

  const loadFlags = useCallback(() => {
    setFlagsLoading(true)
    setError(null)
    integrationsAPI
      .listFeatureFlags()
      .then((r) => setFlags(Array.isArray(r.data) ? r.data : []))
      .catch((e) => {
        setError(getApiErrorMessage(e))
        setFlags([])
      })
      .finally(() => setFlagsLoading(false))
  }, [])

  const loadKeys = useCallback(() => {
    if (!canManageKeys) return
    setKeysLoading(true)
    integrationsAPI
      .listApiKeys()
      .then((r) => setKeys(Array.isArray(r.data) ? r.data : []))
      .catch((e: unknown) => {
        setError(getApiErrorMessage(e))
        setKeys([])
      })
      .finally(() => setKeysLoading(false))
  }, [canManageKeys])

  useEffect(() => {
    loadFlags()
  }, [loadFlags])

  useEffect(() => {
    loadKeys()
  }, [loadKeys])

  const saveFlag = async (e: React.FormEvent) => {
    e.preventDefault()
    const k = flagKey.trim()
    if (!k) return
    setFlagSaving(true)
    setError(null)
    try {
      await integrationsAPI.upsertFeatureFlag({
        flagKey: k,
        enabled: flagEnabled,
        description: flagDesc.trim() || undefined,
      })
      setFlagKey('')
      setFlagDesc('')
      setFlagEnabled(false)
      loadFlags()
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setFlagSaving(false)
    }
  }

  const toggleFlag = async (row: FeatureFlagDto) => {
    setError(null)
    try {
      await integrationsAPI.upsertFeatureFlag({
        flagKey: row.flagKey,
        enabled: !row.enabled,
        description: row.description ?? undefined,
      })
      loadFlags()
    } catch (err) {
      setError(getApiErrorMessage(err))
    }
  }

  const createKey = async (e: React.FormEvent) => {
    e.preventDefault()
    const n = keyName.trim()
    if (!n) return
    setKeyCreating(true)
    setError(null)
    try {
      const res = await integrationsAPI.createApiKey({ name: n })
      setCreatedSecret(res.data as IntegrationApiKeyCreatedDto)
      setKeyName('')
      loadKeys()
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setKeyCreating(false)
    }
  }

  const revokeKey = async (id: string) => {
    if (!window.confirm(t('integrationsPage.confirmRevoke'))) return
    setRevokingId(id)
    setError(null)
    try {
      await integrationsAPI.revokeApiKey(id)
      loadKeys()
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setRevokingId(null)
    }
  }

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('title.integrations')}</h1>
      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      <Card className="mt-8">
        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('integrationsPage.flagsTitle')}</h2>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{t('integrationsPage.flagsBody')}</p>

        <form onSubmit={saveFlag} className="mt-6 flex flex-wrap items-end gap-3">
          <div>
            <label className="block text-xs font-medium text-slate-500 dark:text-slate-400">{t('integrationsPage.flagKey')}</label>
            <input
              value={flagKey}
              onChange={(e) => setFlagKey(e.target.value)}
              placeholder="landing.soft_banner"
              className="mt-1 w-56 rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            />
          </div>
          <div className="flex-1 min-w-[12rem]">
            <label className="block text-xs font-medium text-slate-500 dark:text-slate-400">{t('integrationsPage.flagDesc')}</label>
            <input
              value={flagDesc}
              onChange={(e) => setFlagDesc(e.target.value)}
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            />
          </div>
          <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
            <input type="checkbox" checked={flagEnabled} onChange={(e) => setFlagEnabled(e.target.checked)} />
            {t('integrationsPage.enabled')}
          </label>
          <button
            type="submit"
            disabled={flagSaving || !flagKey.trim()}
            className="dashboard-btn-primary shrink-0 disabled:opacity-50"
          >
            {t('integrationsPage.saveFlag')}
          </button>
        </form>

        <div className="mt-6 table-shell overflow-x-auto">
          {flagsLoading ? (
            <div className="p-6 text-center text-slate-500">{t('ui.loading')}</div>
          ) : flags.length === 0 ? (
            <div className="p-6 text-center text-slate-500">{t('integrationsPage.noFlags')}</div>
          ) : (
            <table className="min-w-full">
              <thead>
                <tr>
                  <th className="px-4 py-3 text-left text-sm">{t('integrationsPage.colKey')}</th>
                  <th className="px-4 py-3 text-left text-sm">{t('integrationsPage.colEnabled')}</th>
                  <th className="px-4 py-3 text-left text-sm">{t('integrationsPage.colDesc')}</th>
                  <th className="px-4 py-3 text-left text-sm">{t('integrationsPage.colActions')}</th>
                </tr>
              </thead>
              <tbody>
                {flags.map((f) => (
                  <tr key={f.flagKey}>
                    <td className="px-4 py-2 font-mono text-sm text-slate-900 dark:text-slate-100">{f.flagKey}</td>
                    <td className="px-4 py-2 text-sm">{f.enabled ? t('integrationsPage.on') : t('integrationsPage.off')}</td>
                    <td className="px-4 py-2 text-sm text-slate-600 dark:text-slate-300">{f.description ?? '—'}</td>
                    <td className="px-4 py-2">
                      <button
                        type="button"
                        onClick={() => toggleFlag(f)}
                        className="text-sm text-primary hover:underline"
                      >
                        {f.enabled ? t('integrationsPage.disable') : t('integrationsPage.enable')}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </Card>

      {canManageKeys && (
        <Card className="mt-8">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('integrationsPage.keysTitle')}</h2>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{t('integrationsPage.keysBody')}</p>

          <form onSubmit={createKey} className="mt-6 flex flex-wrap items-end gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-500 dark:text-slate-400">{t('integrationsPage.keyName')}</label>
              <input
                value={keyName}
                onChange={(e) => setKeyName(e.target.value)}
                className="mt-1 w-64 rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              />
            </div>
            <button
              type="submit"
              disabled={keyCreating || !keyName.trim()}
              className="dashboard-btn-primary shrink-0 disabled:opacity-50"
            >
              {t('integrationsPage.createKey')}
            </button>
          </form>

          <div className="mt-6 table-shell overflow-x-auto">
            {keysLoading ? (
              <div className="p-6 text-center text-slate-500">{t('ui.loading')}</div>
            ) : keys.length === 0 ? (
              <div className="p-6 text-center text-slate-500">{t('integrationsPage.noKeys')}</div>
            ) : (
              <table className="min-w-full">
                <thead>
                  <tr>
                    <th className="px-4 py-3 text-left text-sm">{t('integrationsPage.keyName')}</th>
                    <th className="px-4 py-3 text-left text-sm">{t('integrationsPage.colPrefix')}</th>
                    <th className="px-4 py-3 text-left text-sm">{t('integrationsPage.colCreated')}</th>
                    <th className="px-4 py-3 text-left text-sm">{t('integrationsPage.colActions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {keys.map((k) => (
                    <tr key={k.id}>
                      <td className="px-4 py-2 text-sm">{k.name}</td>
                      <td className="px-4 py-2 font-mono text-xs text-slate-600 dark:text-slate-400">{k.keyPrefix}…</td>
                      <td className="px-4 py-2 text-sm text-slate-600 dark:text-slate-300">{k.createdAt ?? '—'}</td>
                      <td className="px-4 py-2">
                        <button
                          type="button"
                          disabled={revokingId === k.id}
                          onClick={() => revokeKey(k.id)}
                          className="text-sm text-red-600 hover:underline dark:text-red-400"
                        >
                          {t('integrationsPage.revoke')}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </Card>
      )}

      <Modal
        open={!!createdSecret}
        onClose={() => setCreatedSecret(null)}
        title={t('integrationsPage.secretTitle')}
        description={t('integrationsPage.secretWarn')}
        size="sm"
        footer={
          <button type="button" onClick={() => setCreatedSecret(null)} className="dashboard-btn-primary">
            {t('ui.close')}
          </button>
        }
      >
        <textarea
          readOnly
          rows={4}
          value={createdSecret?.plainSecret ?? ''}
          className="modal-textarea font-mono text-xs"
        />
      </Modal>
    </>
  )
}
