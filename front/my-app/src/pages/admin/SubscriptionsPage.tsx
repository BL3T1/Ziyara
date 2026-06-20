/**
 * Admin > Provider Subscriptions — view and manage subscription plans per provider.
 */

import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { subscriptionsAPI, getApiErrorMessage } from '../../services/api'
import type { ProviderSubscriptionDto } from '../../types/api'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'
import { usePermission } from '../../hooks/usePermission'

type PlanTab = 'all' | 'FREE' | 'PRO'

export function SubscriptionsPage() {
  const { t } = useLanguage()
  const canWrite = usePermission('settings:write')
  const [rows, setRows] = useState<(ProviderSubscriptionDto & { providerName?: string })[]>([])
  const [loading, setLoading] = useState(true)
  const [editRow, setEditRow] = useState<(ProviderSubscriptionDto & { providerName?: string }) | null>(null)
  const [error, setError] = useState('')
  const [tab, setTab] = useState<PlanTab>('all')

  const load = () => {
    setLoading(true)
    subscriptionsAPI
      .list()
      .then((res) => {
        const data = res.data
        setRows(Array.isArray(data) ? data : [])
      })
      .catch((e) => {
        setError(getApiErrorMessage(e, 'Failed to load subscriptions'))
        setRows([])
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [])

  const TABS: { id: PlanTab; label: string }[] = [
    { id: 'all',  label: t('subscriptionsPage.tabAll') },
    { id: 'FREE', label: t('subscriptionsPage.tabFree') },
    { id: 'PRO',  label: t('subscriptionsPage.tabPro') },
  ]

  const visibleRows = tab === 'all' ? rows : rows.filter((r) => r.plan === tab)

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="app-page-title">{t('subscriptionsPage.title')}</h1>
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      {/* Plan tabs */}
      <div className="mt-5 flex gap-1 border-b border-slate-200 dark:border-slate-700">
        {TABS.map((tb) => {
          const count = tb.id === 'all' ? rows.length : rows.filter((r) => r.plan === tb.id).length
          return (
            <button
              key={tb.id}
              type="button"
              onClick={() => setTab(tb.id)}
              className={`rounded-t-lg px-4 py-2 text-sm font-medium transition-colors ${
                tab === tb.id
                  ? 'border-b-2 border-primary text-primary dark:text-secondary'
                  : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
              }`}
            >
              {tb.label}
              {count > 0 && (
                <span className="ms-1.5 rounded-full bg-slate-100 px-1.5 py-0.5 text-xs font-medium text-slate-600 dark:bg-slate-700 dark:text-slate-300">
                  {count}
                </span>
              )}
            </button>
          )
        })}
      </div>

      <div className="mt-4 table-shell overflow-x-auto">
        {loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('subscriptionsPage.loading')}</div>
        ) : visibleRows.length === 0 ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('subscriptionsPage.empty')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('subscriptionsPage.colProvider')}</th>
                <th className="px-4 py-3.5">{t('subscriptionsPage.colPlan')}</th>
                <th className="px-4 py-3.5">{t('subscriptionsPage.colStaffLimit')}</th>
                <th className="px-4 py-3.5 text-end">{t('subscriptionsPage.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {visibleRows.map((row) => (
                <tr key={row.id}>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-800 dark:text-slate-100">
                    {row.providerName ?? row.providerId}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${row.plan === 'PRO' ? 'bg-primary/10 text-primary dark:bg-primary/20 dark:text-secondary' : 'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-300'}`}>
                      {row.plan}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {row.staffLimit}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-end text-sm">
                    {canWrite && (
                      <button
                        type="button"
                        onClick={() => {
                          setError('')
                          setEditRow(row)
                        }}
                        className="text-primary hover:underline"
                      >
                        {t('providersPage.viewEdit')}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {editRow && (
        <EditSubscriptionModal
          row={editRow}
          onClose={() => setEditRow(null)}
          onSuccess={() => {
            setEditRow(null)
            setError('')
            load()
          }}
          setError={setError}
        />
      )}
    </>
  )
}

function EditSubscriptionModal({
  row,
  onClose,
  onSuccess,
  setError,
}: {
  row: ProviderSubscriptionDto & { providerName?: string }
  onClose: () => void
  onSuccess: () => void
  setError: (s: string) => void
}) {
  const { t } = useLanguage()
  const [plan, setPlan] = useState(row.plan)
  const [staffLimit, setStaffLimit] = useState(String(row.staffLimit))
  const [submitting, setSubmitting] = useState(false)
  const [localError, setLocalError] = useState('')

  const effectiveLimit = plan === 'FREE' ? 10 : parseInt(staffLimit, 10)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLocalError('')
    setError('')
    const limit = plan === 'FREE' ? 10 : parseInt(staffLimit, 10)
    if (Number.isNaN(limit) || limit < 1) {
      setLocalError('Staff limit must be at least 1')
      return
    }
    setSubmitting(true)
    try {
      await subscriptionsAPI.upsert(row.providerId, { plan, staffLimit: limit })
      onSuccess()
    } catch (err) {
      const msg = getApiErrorMessage(err)
      setLocalError(msg)
      setError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={t('subscriptionsPage.editTitle', { name: row.providerName ?? row.providerId })}
      size="sm"
      footer={
        <>
          <button type="button" onClick={onClose} disabled={submitting} className="dashboard-btn-secondary">
            {t('subscriptionsPage.cancel')}
          </button>
          <button type="submit" form="sub-edit-form" disabled={submitting} className="dashboard-btn-primary disabled:opacity-70">
            {t('subscriptionsPage.save')}
          </button>
        </>
      }
    >
      {localError && (
        <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
          {localError}
        </div>
      )}
      <form id="sub-edit-form" onSubmit={handleSubmit} className="space-y-4">
        <FormField label={t('subscriptionsPage.planLabel')}>
          <select
            value={plan}
            onChange={(e) => setPlan(e.target.value)}
            className="modal-select"
          >
            <option value="FREE">FREE — up to 10 staff</option>
            <option value="PRO">PRO — custom limit</option>
          </select>
        </FormField>
        {plan === 'PRO' && (
          <FormField label={t('subscriptionsPage.staffLimitLabel')} hint={t('subscriptionsPage.staffLimitHint')}>
            <input
              type="number"
              min="1"
              value={staffLimit}
              onChange={(e) => setStaffLimit(e.target.value)}
              className="modal-input"
              required
            />
          </FormField>
        )}
        {plan === 'FREE' && (
          <p className="text-sm text-slate-500 dark:text-slate-400">
            FREE plan: staff limit fixed at {effectiveLimit}.
          </p>
        )}
      </form>
    </Modal>
  )
}
