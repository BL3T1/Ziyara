/**
 * Admin: cash reconciliation queue.
 * Lists pending cash collections from providers and allows admins to reconcile or dispute them.
 */

import { useCallback, useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, adminCashAPI } from '../../services/api'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'

interface CashCollectionRow {
  id: string
  receiptNumber?: string | null
  bookingId?: string | null
  bookingReference?: string | null
  providerId?: string | null
  providerName?: string | null
  amount: number
  currency?: string | null
  notes?: string | null
  status: string
  collectedAt?: string | null
  reconciledAt?: string | null
}

type Tab = 'pending' | 'reconciled'

const inputCls =
  'w-full rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100'

function StatusBadge({ status, t }: { status: string; t: (k: string) => string }) {
  const s = status.toUpperCase()
  if (s === 'RECONCILED') return <span className="badge badge-success">{t('adminCash.tabReconciled')}</span>
  if (s === 'DISPUTED') return <span className="badge badge-danger">{t('ui.reason')}</span>
  return <span className="badge badge-warning">{t('adminCash.tabPending')}</span>
}

export function AdminCashReconciliationPage() {
  const { t } = useLanguage()
  const [tab, setTab] = useState<Tab>('pending')
  const [rows, setRows] = useState<CashCollectionRow[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  // Reconcile modal
  const [reconcileTarget, setReconcileTarget] = useState<CashCollectionRow | null>(null)
  const [reconcileNotes, setReconcileNotes] = useState('')
  const [reconcileSaving, setReconcileSaving] = useState(false)

  // Dispute modal
  const [disputeTarget, setDisputeTarget] = useState<CashCollectionRow | null>(null)
  const [disputeReason, setDisputeReason] = useState('')
  const [disputeSaving, setDisputeSaving] = useState(false)

  const load = useCallback(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    setSuccessMsg(null)
    adminCashAPI
      .listPending()
      .then((res) => {
        if (!cancelled) {
          const all = Array.isArray(res.data) ? (res.data as CashCollectionRow[]) : []
          setRows(all)
        }
      })
      .catch((e) => {
        if (!cancelled) {
          setRows([])
          setError(getApiErrorMessage(e))
        }
      })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [])

  useEffect(load, [load])

  const pending = rows.filter((r) => r.status.toUpperCase() === 'OPEN')
  const reconciled = rows.filter((r) => r.status.toUpperCase() === 'RECONCILED')
  const displayed = tab === 'pending' ? pending : reconciled

  const handleReconcile = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!reconcileTarget) return
    setReconcileSaving(true)
    setError(null)
    try {
      await adminCashAPI.reconcile(reconcileTarget.id, { notes: reconcileNotes.trim() || undefined })
      setSuccessMsg(t('adminCash.reconcileSuccess'))
      setReconcileTarget(null)
      setReconcileNotes('')
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setReconcileSaving(false)
    }
  }

  const handleDispute = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!disputeTarget || !disputeReason.trim()) return
    setDisputeSaving(true)
    setError(null)
    try {
      await adminCashAPI.dispute(disputeTarget.id, { reason: disputeReason.trim() })
      setSuccessMsg(t('adminCash.disputeSuccess'))
      setDisputeTarget(null)
      setDisputeReason('')
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setDisputeSaving(false)
    }
  }

  return (
    <>
      <h1 className="app-page-title">{t('adminCash.title')}</h1>

      {/* Tabs */}
      <div className="mt-4 flex gap-2">
        {(['pending', 'reconciled'] as Tab[]).map((tabId) => (
          <button
            key={tabId}
            type="button"
            onClick={() => setTab(tabId)}
            className={tab === tabId ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
          >
            {t(`adminCash.tab${tabId.charAt(0).toUpperCase() + tabId.slice(1)}`)}
          </button>
        ))}
      </div>

      {error && (
        <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
          {error}
        </div>
      )}
      {successMsg && (
        <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:border-emerald-800/50 dark:bg-emerald-900/20 dark:text-emerald-300">
          {successMsg}
        </div>
      )}

      <div className="mt-4 table-shell">
        {loading ? (
          <div className="flex flex-col gap-2 p-6">
            {Array.from({ length: 5 }, (_, i) => (
              <div key={i} className="h-10 animate-pulse rounded-lg bg-slate-100 dark:bg-white/[0.04]" />
            ))}
          </div>
        ) : displayed.length === 0 ? (
          <div className="dashboard-empty-state">
            <div className="dashboard-empty-state__icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                <rect x="2" y="5" width="20" height="14" rx="2" />
                <path d="M2 10h20" />
              </svg>
            </div>
            <p className="dashboard-empty-state__title">
              {tab === 'pending' ? t('adminCash.noPending') : t('adminCash.noReconciled')}
            </p>
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('adminCash.colReceipt')}</th>
                <th className="px-4 py-3.5">{t('adminCash.colProvider')}</th>
                <th className="px-4 py-3.5">{t('adminCash.colBooking')}</th>
                <th className="px-4 py-3.5">{t('adminCash.colAmount')}</th>
                <th className="px-4 py-3.5">{t('adminCash.colCollectedAt')}</th>
                <th className="px-4 py-3.5">{t('adminCash.colStatus')}</th>
                {tab === 'pending' && <th className="px-4 py-3.5">{t('adminCash.colActions')}</th>}
              </tr>
            </thead>
            <tbody>
              {displayed.map((row) => (
                <tr key={row.id}>
                  <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-700 dark:text-slate-200">
                    {row.receiptNumber ?? '—'}
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {row.providerName ?? row.providerId ?? '—'}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-600 dark:text-slate-300">
                    {row.bookingReference ?? row.bookingId ?? '—'}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 tabular-nums text-sm font-medium text-slate-800 dark:text-slate-100">
                    {row.currency ?? 'USD'} {row.amount?.toFixed(2)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {row.collectedAt ? new Date(row.collectedAt).toLocaleDateString() : '—'}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    <StatusBadge status={row.status} t={t} />
                  </td>
                  {tab === 'pending' && (
                    <td className="whitespace-nowrap px-4 py-3">
                      <div className="flex gap-3">
                        <button
                          type="button"
                          onClick={() => { setReconcileTarget(row); setReconcileNotes('') }}
                          className="text-sm font-semibold text-emerald-600 hover:underline dark:text-emerald-400"
                        >
                          {t('adminCash.reconcile')}
                        </button>
                        <button
                          type="button"
                          onClick={() => { setDisputeTarget(row); setDisputeReason('') }}
                          className="text-sm font-semibold text-amber-600 hover:underline dark:text-amber-400"
                        >
                          {t('adminCash.dispute')}
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Reconcile modal */}
      <Modal
        open={!!reconcileTarget}
        onClose={() => setReconcileTarget(null)}
        title={t('adminCash.reconcileTitle')}
        size="sm"
        footer={
          <>
            <button
              type="button"
              onClick={() => setReconcileTarget(null)}
              disabled={reconcileSaving}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="reconcile-form"
              disabled={reconcileSaving}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {reconcileSaving ? t('ui.saving') : t('adminCash.reconcile')}
            </button>
          </>
        }
      >
        {reconcileTarget && (
          <form id="reconcile-form" onSubmit={handleReconcile} className="space-y-3">
            <p className="text-sm text-slate-600 dark:text-slate-300">
              {t('adminCash.reconcileConfirm', { receipt: reconcileTarget.receiptNumber ?? reconcileTarget.id })}
            </p>
            <p className="tabular-nums text-base font-bold text-slate-900 dark:text-slate-100">
              {reconcileTarget.currency ?? 'USD'} {reconcileTarget.amount?.toFixed(2)}
            </p>
            <FormField label={t('adminCash.notesPlaceholder')}>
              <input
                type="text"
                value={reconcileNotes}
                onChange={(e) => setReconcileNotes(e.target.value)}
                className={inputCls}
                placeholder={t('adminCash.notesPlaceholder')}
              />
            </FormField>
          </form>
        )}
      </Modal>

      {/* Dispute modal */}
      <Modal
        open={!!disputeTarget}
        onClose={() => setDisputeTarget(null)}
        title={t('adminCash.disputeTitle')}
        size="sm"
        footer={
          <>
            <button
              type="button"
              onClick={() => setDisputeTarget(null)}
              disabled={disputeSaving}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="dispute-form"
              disabled={disputeSaving || !disputeReason.trim()}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {disputeSaving ? t('ui.saving') : t('adminCash.dispute')}
            </button>
          </>
        }
      >
        {disputeTarget && (
          <form id="dispute-form" onSubmit={handleDispute} className="space-y-3">
            <p className="tabular-nums text-base font-bold text-slate-900 dark:text-slate-100">
              {disputeTarget.receiptNumber ?? disputeTarget.id} — {disputeTarget.currency ?? 'USD'} {disputeTarget.amount?.toFixed(2)}
            </p>
            <FormField label={t('adminCash.disputeReason')}>
              <textarea
                required
                rows={3}
                value={disputeReason}
                onChange={(e) => setDisputeReason(e.target.value)}
                className={inputCls}
                placeholder={t('adminCash.disputeReasonPlaceholder')}
              />
            </FormField>
          </form>
        )}
      </Modal>
    </>
  )
}
