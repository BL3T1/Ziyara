import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useLanguage } from '../../context/LanguageContext'
import { useDisplayCurrency } from '../../context/DisplayCurrencyContext'
import { adminPayoutsAPI, providersAPI, getApiErrorMessage } from '../../services/api'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'
import { usePermission } from '../../hooks/usePermission'
import type {
  AdminPayoutDto,
  AdminPayoutSummaryDto,
  BulkPayoutResultDto,
  PageDto,
  PayoutStatus,
  ServiceProviderDto,
} from '../../types/api'

// ─── helpers ───────────────────────────────────────────────────────────────

const PAGE_SIZE = 50
const POLL_INTERVAL = 30_000

function fmt(iso?: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

function fmtDatetime(iso?: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

// ─── Status badge ───────────────────────────────────────────────────────────

const STATUS_COLORS: Record<PayoutStatus, string> = {
  PENDING:    'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300',
  ON_HOLD:    'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-300',
  SCHEDULED:  'bg-sky-100 text-sky-800 dark:bg-sky-900/30 dark:text-sky-300',
  PROCESSING: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
  COMPLETED:  'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
  FAILED:     'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300',
  CANCELLED:  'bg-slate-300 text-slate-600 dark:bg-slate-800 dark:text-slate-400',
  REJECTED:   'bg-rose-100 text-rose-800 dark:bg-rose-900/30 dark:text-rose-300',
}

function usePayoutStatusLabel() {
  const { t } = useLanguage()
  return useCallback((status: string): string => {
    const map: Record<string, string> = {
      PENDING:    t('payoutsPage.statusPending'),
      ON_HOLD:    t('payoutsPage.statusOnHold'),
      SCHEDULED:  t('payoutsPage.statusScheduled'),
      PROCESSING: t('payoutsPage.statusProcessing'),
      COMPLETED:  t('payoutsPage.statusCompleted'),
      FAILED:     t('payoutsPage.statusFailed'),
      CANCELLED:  t('payoutsPage.statusCancelled'),
      REJECTED:   t('payoutsPage.statusRejected'),
    }
    return map[status] ?? status
  }, [t])
}

function StatusBadge({ status }: { status: string }) {
  const label = usePayoutStatusLabel()
  const colorClass = STATUS_COLORS[status as PayoutStatus] ?? 'bg-slate-100 text-slate-600'
  return (
    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${colorClass}`}>
      {label(status)}
    </span>
  )
}

// ─── Summary card ───────────────────────────────────────────────────────────

function SummaryCard({
  label, value, sub, color, active, onClick,
}: {
  label: string
  value: string | number
  sub?: string
  color: string
  active?: boolean
  onClick?: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={[
        'rounded-xl border p-4 text-left transition-all',
        'hover:shadow-md dark:hover:shadow-slate-900',
        active
          ? 'border-primary/40 bg-primary/5 shadow-sm dark:bg-primary/10'
          : 'border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-800',
      ].join(' ')}
    >
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">{label}</p>
      <p className={`mt-1 text-xl font-bold ${color}`}>{value}</p>
      {sub && <p className="mt-0.5 text-xs text-slate-400 dark:text-slate-500">{sub}</p>}
    </button>
  )
}

// ─── Detail drawer ──────────────────────────────────────────────────────────

function DetailDrawer({
  payout,
  onClose,
  onAction,
  canApprove,
}: {
  payout: AdminPayoutDto | null
  onClose: () => void
  onAction: (action: string, id: string) => void
  canApprove: boolean
}) {
  const { t } = useLanguage()
  const { displayInDefault } = useDisplayCurrency()
  const [notes, setNotes] = useState('')
  const [savingNotes, setSavingNotes] = useState(false)
  const statusLabel = usePayoutStatusLabel()

  useEffect(() => {
    if (payout) setNotes(payout.notes ?? '')
  }, [payout?.id])

  if (!payout) return null

  const saveNotes = async () => {
    setSavingNotes(true)
    try {
      await adminPayoutsAPI.updateNotes(payout.id, notes)
    } finally {
      setSavingNotes(false)
    }
  }

  return createPortal(
    <div
      className="fixed inset-0 z-40 flex justify-end"
      onClick={onClose}
    >
      <div
        className="relative h-full w-full max-w-lg overflow-y-auto bg-white shadow-xl dark:bg-slate-900"
        onClick={(e) => e.stopPropagation()}
      >
        {/* header */}
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-200 bg-white px-5 py-4 dark:border-slate-700 dark:bg-slate-900">
          <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100">
            {t('payoutsPage.drawerTitle')}
          </h2>
          <button type="button" onClick={onClose} className="text-slate-400 hover:text-slate-700 dark:hover:text-slate-200">
            <svg width="18" height="18" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path d="M12 4L4 12M4 4l8 8" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" />
            </svg>
          </button>
        </div>

        <div className="space-y-6 p-5">
          {/* provider info */}
          <section>
            <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
              {t('payoutsPage.drawerProvider')}
            </p>
            <p className="font-semibold text-slate-900 dark:text-slate-100">{payout.providerName ?? '—'}</p>
            {payout.providerEmail && <p className="text-sm text-slate-500 dark:text-slate-400">{payout.providerEmail}</p>}
            <p className="mt-0.5 font-mono text-xs text-slate-400">{payout.providerId}</p>
          </section>

          {/* amount & status */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                {t('payoutsPage.drawerAmount')}
              </p>
              <p className="text-lg font-bold text-slate-900 dark:text-slate-100">
                {displayInDefault(payout.amount, payout.currency)}
              </p>
            </div>
            <div>
              <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                {t('payoutsPage.drawerStatus')}
              </p>
              <StatusBadge status={payout.status} />
            </div>
          </div>

          {/* dates */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                {t('payoutsPage.drawerRequestedAt')}
              </p>
              <p className="text-sm text-slate-700 dark:text-slate-300">{fmtDatetime(payout.requestedAt)}</p>
            </div>
            {payout.processedAt && (
              <div>
                <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  {t('payoutsPage.drawerProcessedAt')}
                </p>
                <p className="text-sm text-slate-700 dark:text-slate-300">{fmtDatetime(payout.processedAt)}</p>
              </div>
            )}
          </div>

          {payout.transactionId && (
            <div>
              <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                {t('payoutsPage.drawerTransactionId')}
              </p>
              <p className="font-mono text-sm text-slate-700 dark:text-slate-300">{payout.transactionId}</p>
            </div>
          )}

          {/* notes */}
          <div>
            <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
              {t('payoutsPage.drawerNotes')}
            </p>
            <textarea
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder={t('payoutsPage.drawerNotesPlaceholder')}
              className="modal-textarea w-full"
            />
            <button
              type="button"
              onClick={saveNotes}
              disabled={savingNotes}
              className="mt-1.5 dashboard-btn-secondary text-sm disabled:opacity-50"
            >
              {savingNotes ? t('ui.saving') : t('payoutsPage.drawerSaveNotes')}
            </button>
          </div>

          {/* status history */}
          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
              {t('payoutsPage.drawerHistory')}
            </p>
            {(!payout.statusHistory || payout.statusHistory.length === 0) ? (
              <p className="text-sm text-slate-400 dark:text-slate-500">{t('payoutsPage.drawerNoHistory')}</p>
            ) : (
              <ol className="relative border-l-2 border-slate-200 dark:border-slate-700 pl-4 space-y-3">
                {payout.statusHistory.map((h, i) => (
                  <li key={i} className="relative">
                    <div className="absolute -left-[1.375rem] mt-0.5 h-3 w-3 rounded-full border-2 border-white bg-slate-400 dark:border-slate-900 dark:bg-slate-500" />
                    <p className="text-xs text-slate-500 dark:text-slate-400">{fmtDatetime(h.timestamp)}</p>
                    <p className="text-sm text-slate-700 dark:text-slate-300">
                      <span className="font-medium">{h.fromStatus ? `${statusLabel(h.fromStatus)} → ` : ''}</span>
                      <span className="font-semibold">{statusLabel(h.toStatus)}</span>
                    </p>
                    {h.userDisplay && <p className="text-xs text-slate-400">{h.userDisplay}</p>}
                    {h.notes && <p className="text-xs text-slate-500 italic dark:text-slate-400">{h.notes}</p>}
                  </li>
                ))}
              </ol>
            )}
          </div>

          {/* contextual action */}
          {canApprove && (
            <div className="flex flex-wrap gap-2 border-t border-slate-200 pt-4 dark:border-slate-700">
              {payout.status === 'PENDING' && (
                <>
                  <button type="button" onClick={() => onAction('pay', payout.id)} className="dashboard-btn-primary text-sm">
                    {t('payoutsPage.actionPayNow')}
                  </button>
                  <button type="button" onClick={() => onAction('hold', payout.id)} className="dashboard-btn-secondary text-sm">
                    {t('payoutsPage.actionHold')}
                  </button>
                </>
              )}
              {payout.status === 'ON_HOLD' && (
                <>
                  <button type="button" onClick={() => onAction('release', payout.id)} className="dashboard-btn-secondary text-sm">
                    {t('payoutsPage.actionReleaseHold')}
                  </button>
                  <button type="button" onClick={() => onAction('cancel', payout.id)} className="dashboard-btn-secondary text-sm text-red-600">
                    {t('payoutsPage.actionCancel')}
                  </button>
                </>
              )}
              {(payout.status === 'FAILED' || payout.status === 'REJECTED') && (
                <button type="button" onClick={() => onAction('retry', payout.id)} className="dashboard-btn-secondary text-sm">
                  {t('payoutsPage.actionRetry')}
                </button>
              )}
              {payout.status === 'PROCESSING' && (
                <button type="button" onClick={() => onAction('markPaid', payout.id)} className="dashboard-btn-secondary text-sm">
                  {t('payoutsPage.actionMarkPaid')}
                </button>
              )}
            </div>
          )}
        </div>
      </div>
    </div>,
    document.body,
  )
}

// ─── Main page ───────────────────────────────────────────────────────────────

export function PayoutsPage() {
  const { t } = useLanguage()
  const { displayInDefault } = useDisplayCurrency()
  const canApprove = usePermission('payouts:approve')
  const canWrite = usePermission('payouts:write')
  const canExport = usePermission('reports:read')
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // list state
  const [payouts, setPayouts] = useState<AdminPayoutDto[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // filters
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [search, setSearch] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [datePreset, setDatePreset] = useState<'today' | 'week' | 'month' | 'custom' | ''>('')
  const [dateStart, setDateStart] = useState('')
  const [dateEnd, setDateEnd] = useState('')

  // summary
  const [summary, setSummary] = useState<AdminPayoutSummaryDto | null>(null)
  const [summaryLoading, setSummaryLoading] = useState(true)

  // selection
  const [selected, setSelected] = useState<Set<string>>(new Set())

  // drawers & modals
  const [detailPayout, setDetailPayout] = useState<AdminPayoutDto | null>(null)
  const [payModal, setPayModal] = useState<{ ids: string[]; payouts: AdminPayoutDto[] } | null>(null)
  const [payLoading, setPayLoading] = useState(false)
  const [payResult, setPayResult] = useState<BulkPayoutResultDto | null>(null)
  const [manualModal, setManualModal] = useState(false)
  const [markPaidModal, setMarkPaidModal] = useState<{ id: string } | null>(null)
  const [cancelConfirm, setCancelConfirm] = useState<{ id: string } | null>(null)
  const [scheduleModal, setScheduleModal] = useState<{ id: string } | null>(null)
  const [scheduleDate, setScheduleDate] = useState('')

  // manual payment form
  const [manualProvider, setManualProvider] = useState('')
  const [manualProviderSearch, setManualProviderSearch] = useState('')
  const [manualAmount, setManualAmount] = useState('')
  const [manualCurrency, setManualCurrency] = useState('USD')
  const [manualMemo, setManualMemo] = useState('')
  const [manualExecuteNow, setManualExecuteNow] = useState(false)
  const [manualLoading, setManualLoading] = useState(false)
  const [providerList, setProviderList] = useState<Pick<ServiceProviderDto, 'id' | 'name' | 'type'>[]>([])
  const [providerListLoading, setProviderListLoading] = useState(false)

  // mark paid form
  const [markPaidTxn, setMarkPaidTxn] = useState('')
  const [markPaidNotes, setMarkPaidNotes] = useState('')

  // resolved date range from preset
  const resolvedDates = useMemo(() => {
    if (datePreset === 'today') {
      const d = new Date().toISOString().split('T')[0]
      return { start: d, end: d }
    }
    if (datePreset === 'week') {
      const now = new Date()
      const mon = new Date(now)
      mon.setDate(now.getDate() - ((now.getDay() + 6) % 7))
      return { start: mon.toISOString().split('T')[0], end: now.toISOString().split('T')[0] }
    }
    if (datePreset === 'month') {
      const now = new Date()
      return { start: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`, end: now.toISOString().split('T')[0] }
    }
    if (datePreset === 'custom') {
      return { start: dateStart, end: dateEnd }
    }
    return { start: '', end: '' }
  }, [datePreset, dateStart, dateEnd])

  const loadSummary = useCallback(() => {
    setSummaryLoading(true)
    adminPayoutsAPI.getSummary({ start: resolvedDates.start || undefined, end: resolvedDates.end || undefined })
      .then((res) => setSummary(res.data as AdminPayoutSummaryDto))
      .catch(() => {})
      .finally(() => setSummaryLoading(false))
  }, [resolvedDates.start, resolvedDates.end])

  const loadPayouts = useCallback(() => {
    setLoading(true)
    setError(null)
    adminPayoutsAPI
      .list({
        page,
        size: PAGE_SIZE,
        status: statusFilter || undefined,
        start: resolvedDates.start || undefined,
        end: resolvedDates.end || undefined,
        q: search || undefined,
      })
      .then((res) => {
        const p = res.data as PageDto<AdminPayoutDto>
        setPayouts(p?.content ?? [])
        setTotalPages(p?.totalPages ?? 0)
      })
      .catch((e: unknown) => {
        setError(getApiErrorMessage(e))
        setPayouts([])
        setTotalPages(0)
      })
      .finally(() => setLoading(false))
  }, [page, statusFilter, resolvedDates.start, resolvedDates.end, search])

  useEffect(() => { loadSummary() }, [loadSummary])
  useEffect(() => { loadPayouts(); setSelected(new Set()) }, [loadPayouts])

  // polling
  useEffect(() => {
    if (pollRef.current) clearInterval(pollRef.current)
    pollRef.current = setInterval(() => { loadSummary(); loadPayouts() }, POLL_INTERVAL)
    return () => { if (pollRef.current) clearInterval(pollRef.current) }
  }, [loadSummary, loadPayouts])

  // search debounce
  useEffect(() => {
    const t = setTimeout(() => setSearch(searchInput.trim()), 350)
    return () => clearTimeout(t)
  }, [searchInput])

  // ─── selection helpers ────────────────────────────────────────────────────

  const pendingIds = useMemo(() => payouts.filter((p) => p.status === 'PENDING').map((p) => p.id), [payouts])
  const allPendingSelected = pendingIds.length > 0 && pendingIds.every((id) => selected.has(id))

  const toggleAll = () => {
    if (allPendingSelected) {
      setSelected((prev) => { const s = new Set(prev); pendingIds.forEach((id) => s.delete(id)); return s })
    } else {
      setSelected((prev) => { const s = new Set(prev); pendingIds.forEach((id) => s.add(id)); return s })
    }
  }

  const toggleRow = (id: string) => {
    setSelected((prev) => { const s = new Set(prev); s.has(id) ? s.delete(id) : s.add(id); return s })
  }

  // ─── row actions ─────────────────────────────────────────────────────────

  const handleRowAction = async (action: string, id: string) => {
    try {
      if (action === 'pay') {
        const p = payouts.find((x) => x.id === id)
        if (p) setPayModal({ ids: [id], payouts: [p] })
      } else if (action === 'hold') {
        await adminPayoutsAPI.hold(id)
        refresh()
      } else if (action === 'release') {
        await adminPayoutsAPI.releaseHold(id)
        refresh()
      } else if (action === 'cancel') {
        setCancelConfirm({ id })
      } else if (action === 'retry') {
        await adminPayoutsAPI.retry(id)
        refresh()
      } else if (action === 'markPaid') {
        setMarkPaidModal({ id })
      } else if (action === 'schedule') {
        setScheduleModal({ id })
      }
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  const refresh = () => { loadPayouts(); loadSummary() }

  // ─── bulk pay ─────────────────────────────────────────────────────────────

  const openBulkPay = () => {
    const sel = payouts.filter((p) => selected.has(p.id) && p.status === 'PENDING')
    if (!sel.length) return
    setPayModal({ ids: sel.map((p) => p.id), payouts: sel })
  }

  const executePay = async () => {
    if (!payModal) return
    setPayLoading(true)
    try {
      const res = await adminPayoutsAPI.bulkApprove({ ids: payModal.ids })
      setPayResult(res.data as BulkPayoutResultDto)
      refresh()
    } catch (e) {
      setError(getApiErrorMessage(e))
      setPayModal(null)
    } finally {
      setPayLoading(false)
    }
  }

  // ─── manual payout ────────────────────────────────────────────────────────

  const openManualModal = () => {
    setManualModal(true)
    setManualProvider('')
    setManualProviderSearch('')
    setManualAmount('')
    setManualCurrency('USD')
    setManualMemo('')
    setManualExecuteNow(false)
    setProviderListLoading(true)
    providersAPI
      .list({ size: 200, status: 'ACTIVE' })
      .then((res) => {
        const page = res.data as { content?: ServiceProviderDto[] }
        const items = Array.isArray(page?.content) ? page.content : (Array.isArray(res.data) ? res.data as ServiceProviderDto[] : [])
        setProviderList(items.map((p) => ({ id: p.id, name: p.name ?? p.id, type: p.type })))
      })
      .catch(() => setProviderList([]))
      .finally(() => setProviderListLoading(false))
  }

  const closeManualModal = () => {
    setManualModal(false)
    setManualProvider('')
    setManualProviderSearch('')
    setManualAmount('')
    setManualMemo('')
    setManualExecuteNow(false)
  }

  const submitManual = async () => {
    if (!manualProvider.trim() || !manualAmount) return
    setManualLoading(true)
    try {
      await adminPayoutsAPI.createManual({
        providerId: manualProvider.trim(),
        amount: parseFloat(manualAmount),
        currency: manualCurrency || 'USD',
        memo: manualMemo.trim() || undefined,
        executeImmediately: manualExecuteNow,
      })
      closeManualModal()
      refresh()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setManualLoading(false)
    }
  }

  // ─── export ───────────────────────────────────────────────────────────────

  const handleExport = async () => {
    try {
      const res = await adminPayoutsAPI.export({
        status: statusFilter || undefined,
        start: resolvedDates.start || undefined,
        end: resolvedDates.end || undefined,
      })
      const url = URL.createObjectURL(res.data as Blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `payouts-${new Date().toISOString().split('T')[0]}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  // ─── status card filters ──────────────────────────────────────────────────

  const handleCardFilter = (s: string) => {
    setStatusFilter((prev) => (prev === s ? '' : s))
    setPage(0)
  }

  const summaryCards = useMemo(() => [
    {
      key: 'PENDING',
      label: t('payoutsPage.cardTotalPayable'),
      value: summary ? displayInDefault(summary.totalPayable, summary.currency) : '—',
      sub: summary ? `${summary.pendingCount} ${t('payoutsPage.cardPending').toLowerCase()}` : undefined,
      color: 'text-amber-700 dark:text-amber-400',
    },
    {
      key: 'PENDING_COUNT',
      label: t('payoutsPage.cardPending'),
      value: summaryLoading ? '…' : (summary?.pendingCount ?? 0),
      color: 'text-amber-700 dark:text-amber-400',
    },
    {
      key: 'PROCESSING',
      label: t('payoutsPage.cardProcessing'),
      value: summaryLoading ? '…' : (summary?.processingCount ?? 0),
      color: 'text-blue-700 dark:text-blue-400',
    },
    {
      key: 'COMPLETED',
      label: t('payoutsPage.cardCompleted'),
      value: summary ? displayInDefault(summary.totalCompletedInPeriod, summary.currency) : '—',
      color: 'text-green-700 dark:text-green-400',
    },
    {
      key: 'FAILED',
      label: t('payoutsPage.cardFailed'),
      value: summaryLoading ? '…' : (summary?.failedOnHoldCount ?? 0),
      color: 'text-red-700 dark:text-red-400',
    },
    {
      key: '',
      label: t('payoutsPage.cardPlatformFees'),
      value: '—',
      color: 'text-slate-700 dark:text-slate-300',
    },
  ], [t, summary, summaryLoading, displayInDefault])

  const statusTabs: Array<{ key: string; label: string }> = useMemo(() => [
    { key: '', label: t('ui.all') },
    { key: 'PENDING', label: t('payoutsPage.statusPending') },
    { key: 'ON_HOLD', label: t('payoutsPage.statusOnHold') },
    { key: 'PROCESSING', label: t('payoutsPage.statusProcessing') },
    { key: 'COMPLETED', label: t('payoutsPage.statusCompleted') },
    { key: 'FAILED', label: t('payoutsPage.statusFailed') },
    { key: 'CANCELLED', label: t('payoutsPage.statusCancelled') },
  ], [t])

  const selectedArr = Array.from(selected)

  return (
    <>
      {/* ── Page header ──────────────────────────────────────────────── */}
      <div className="mb-6">
        <p className="text-xs text-slate-400 dark:text-slate-500">{t('payoutsPage.breadcrumb')}</p>
        <h1 className="mt-0.5 text-2xl font-bold text-slate-900 dark:text-slate-100">{t('payoutsPage.title')}</h1>
        <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">{t('payoutsPage.subtitle')}</p>
      </div>

      {error && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      {/* ── Action bar ───────────────────────────────────────────────── */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
        {/* Date presets */}
        <div className="flex gap-1.5">
          {(['today', 'week', 'month', 'custom'] as const).map((p) => (
            <button
              key={p}
              type="button"
              onClick={() => { setDatePreset((prev) => (prev === p ? '' : p)); setPage(0) }}
              className={datePreset === p ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
            >
              {t(`payoutsPage.date${p.charAt(0).toUpperCase() + p.slice(1)}` as Parameters<typeof t>[0])}
            </button>
          ))}
        </div>

        {datePreset === 'custom' && (
          <div className="flex gap-2">
            <input type="date" value={dateStart} onChange={(e) => { setDateStart(e.target.value); setPage(0) }}
              className="rounded border border-slate-300 px-2 py-1 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100" />
            <input type="date" value={dateEnd} onChange={(e) => { setDateEnd(e.target.value); setPage(0) }}
              className="rounded border border-slate-300 px-2 py-1 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100" />
          </div>
        )}

        {/* Search */}
        <div className="relative flex-1 min-w-[200px] max-w-xs">
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder={t('payoutsPage.searchPlaceholder')}
            className="w-full rounded-lg border border-slate-300 py-1.5 pl-8 pr-3 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500"
          />
          <svg className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <circle cx="6.5" cy="6.5" r="4.5" stroke="currentColor" strokeWidth="1.5" />
            <path d="M10 10l3 3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
          </svg>
        </div>

        <div className="ml-auto flex gap-2">
          {canWrite && (
            <button type="button" onClick={openManualModal} className="dashboard-btn-primary text-sm">
              {t('payoutsPage.newManual')}
            </button>
          )}
          {canExport && (
            <button type="button" onClick={handleExport} className="dashboard-btn-secondary text-sm">
              {t('payoutsPage.exportBtn')}
            </button>
          )}
        </div>
      </div>

      {/* ── Summary cards ────────────────────────────────────────────── */}
      <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        {summaryCards.map((card) => (
          <SummaryCard
            key={card.key}
            label={card.label}
            value={card.value}
            sub={card.sub}
            color={card.color}
            active={statusFilter === card.key && card.key !== ''}
            onClick={card.key ? () => handleCardFilter(card.key) : undefined}
          />
        ))}
      </div>

      {/* ── Status filter tabs ───────────────────────────────────────── */}
      <div className="mb-4 flex flex-wrap gap-1.5">
        {statusTabs.map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => { setStatusFilter(tab.key); setPage(0) }}
            className={statusFilter === tab.key ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* ── Bulk action bar ──────────────────────────────────────────── */}
      {selected.size > 0 && canApprove && (
        <div className="mb-3 flex flex-wrap items-center gap-2 rounded-lg border border-primary/30 bg-primary/5 px-4 py-2.5 dark:bg-primary/10">
          <span className="text-sm font-medium text-slate-700 dark:text-slate-300">
            {t('payoutsPage.totalSelected', { n: selected.size })}
          </span>
          <button type="button" onClick={openBulkPay}
            className="dashboard-btn-primary text-sm">
            {t('payoutsPage.bulkPay', { n: selected.size })}
          </button>
          <button type="button"
            onClick={async () => { await adminPayoutsAPI.bulkHold({ ids: selectedArr }); refresh() }}
            className="dashboard-btn-secondary text-sm">
            {t('payoutsPage.bulkHold')}
          </button>
          <button type="button"
            onClick={async () => { await adminPayoutsAPI.bulkReleaseHold({ ids: selectedArr }); refresh() }}
            className="dashboard-btn-secondary text-sm">
            {t('payoutsPage.bulkRelease')}
          </button>
          <button type="button" onClick={() => setSelected(new Set())}
            className="ml-auto text-xs text-slate-400 hover:text-slate-600 dark:hover:text-slate-200">
            ✕
          </button>
        </div>
      )}

      {/* ── Table ────────────────────────────────────────────────────── */}
      <div className="table-shell">
        {loading ? (
          <div className="space-y-2 p-6">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="h-10 animate-pulse rounded bg-slate-100 dark:bg-slate-700" />
            ))}
          </div>
        ) : payouts.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-3 p-16 text-slate-400 dark:text-slate-500">
            <svg width="40" height="40" fill="none" viewBox="0 0 24 24" aria-hidden="true">
              <rect x="3" y="6" width="18" height="13" rx="2" stroke="currentColor" strokeWidth="1.5" />
              <path d="M3 10h18" stroke="currentColor" strokeWidth="1.5" />
              <circle cx="7" cy="14" r="1" fill="currentColor" />
            </svg>
            <p className="text-sm">{t('payoutsPage.noPayouts')}</p>
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="w-8 px-3 py-3.5">
                  <input
                    type="checkbox"
                    checked={allPendingSelected}
                    onChange={toggleAll}
                    className="cursor-pointer accent-primary"
                    aria-label="Select all pending"
                  />
                </th>
                <th className="px-4 py-3.5 text-left">{t('payoutsPage.colProvider')}</th>
                <th className="px-4 py-3.5 text-right">{t('payoutsPage.colAmount')}</th>
                <th className="px-4 py-3.5">{t('payoutsPage.colStatus')}</th>
                <th className="px-4 py-3.5">{t('payoutsPage.colRequestedAt')}</th>
                <th className="px-4 py-3.5">{t('payoutsPage.colTransactionId')}</th>
                <th className="px-4 py-3.5">{t('payoutsPage.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {payouts.map((p) => (
                <tr key={p.id} className={selected.has(p.id) ? 'bg-primary/5 dark:bg-primary/10' : ''}>
                  <td className="w-8 px-3 py-3">
                    <input
                      type="checkbox"
                      checked={selected.has(p.id)}
                      onChange={() => toggleRow(p.id)}
                      className="cursor-pointer accent-primary"
                      disabled={!['PENDING'].includes(p.status)}
                    />
                  </td>
                  <td className="px-4 py-3">
                    <div>
                      <p className="text-sm font-medium text-slate-900 dark:text-slate-100">
                        {p.providerName ?? '—'}
                      </p>
                      <p className="font-mono text-xs text-slate-400">{p.providerId.slice(0, 8)}…</p>
                    </div>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right text-sm font-bold text-slate-900 dark:text-slate-100">
                    {displayInDefault(p.amount, p.currency)}
                    {p.manual && (
                      <span className="ml-1.5 rounded bg-violet-100 px-1.5 py-0.5 text-xs font-normal text-violet-700 dark:bg-violet-900/30 dark:text-violet-300">
                        {t('payoutsPage.manual')}
                      </span>
                    )}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    <StatusBadge status={p.status} />
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-500 dark:text-slate-400">
                    {fmt(p.requestedAt)}
                  </td>
                  <td className="max-w-[8rem] truncate px-4 py-3 font-mono text-xs text-slate-500 dark:text-slate-400"
                    title={p.transactionId ?? undefined}>
                    {p.transactionId ?? '—'}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    <div className="flex items-center gap-1.5">
                      <button
                        type="button"
                        onClick={() => setDetailPayout(p)}
                        className="text-xs text-primary hover:underline"
                      >
                        {t('payoutsPage.actionViewDetails')}
                      </button>
                      {canApprove && p.status === 'PENDING' && (
                        <>
                          <span className="text-slate-300 dark:text-slate-600">·</span>
                          <button type="button" onClick={() => setPayModal({ ids: [p.id], payouts: [p] })}
                            className="text-xs font-medium text-emerald-600 hover:underline dark:text-emerald-400">
                            {t('payoutsPage.actionPayNow')}
                          </button>
                          <span className="text-slate-300 dark:text-slate-600">·</span>
                          <button type="button" onClick={() => handleRowAction('hold', p.id)}
                            className="text-xs text-slate-500 hover:underline dark:text-slate-400">
                            {t('payoutsPage.actionHold')}
                          </button>
                        </>
                      )}
                      {canApprove && p.status === 'ON_HOLD' && (
                        <>
                          <span className="text-slate-300 dark:text-slate-600">·</span>
                          <button type="button" onClick={() => handleRowAction('release', p.id)}
                            className="text-xs text-slate-500 hover:underline dark:text-slate-400">
                            {t('payoutsPage.actionReleaseHold')}
                          </button>
                        </>
                      )}
                      {canApprove && (p.status === 'FAILED' || p.status === 'REJECTED') && (
                        <>
                          <span className="text-slate-300 dark:text-slate-600">·</span>
                          <button type="button" onClick={() => handleRowAction('retry', p.id)}
                            className="text-xs text-amber-600 hover:underline dark:text-amber-400">
                            {t('payoutsPage.actionRetry')}
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* ── Pagination ───────────────────────────────────────────────── */}
      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-2">
          <button
            type="button"
            disabled={page <= 0 || loading}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="rounded-lg border-2 border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-800 disabled:opacity-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            {t('ui.previous')}
          </button>
          <span className="text-sm text-slate-500 dark:text-slate-400">
            {t('ui.pageOf', { current: page + 1, total: totalPages })}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1 || loading}
            onClick={() => setPage((p) => p + 1)}
            className="rounded-lg border-2 border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-800 disabled:opacity-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            {t('ui.next')}
          </button>
        </div>
      )}

      {/* ── Detail drawer ────────────────────────────────────────────── */}
      {detailPayout && (
        <DetailDrawer
          payout={detailPayout}
          onClose={() => setDetailPayout(null)}
          canApprove={canApprove}
          onAction={async (action, id) => {
            await handleRowAction(action, id)
            const updated = payouts.find((x) => x.id === id)
            if (updated) setDetailPayout(updated)
          }}
        />
      )}

      {/* ── Pay modal ────────────────────────────────────────────────── */}
      <Modal
        open={!!payModal && !payResult}
        onClose={() => { setPayModal(null); setPayResult(null) }}
        title={t('payoutsPage.payModalTitle')}
        description={t('payoutsPage.payModalSubtitle')}
        size="md"
        footer={
          <>
            <button type="button" onClick={() => setPayModal(null)} className="dashboard-btn-secondary">
              {t('ui.cancel')}
            </button>
            <button type="button" onClick={executePay} disabled={payLoading} className="dashboard-btn-primary disabled:opacity-50">
              {payLoading ? t('payoutsPage.payModalProcessing') : t('payoutsPage.payModalConfirm')}
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div className="max-h-48 overflow-y-auto rounded border border-slate-200 dark:border-slate-700">
            {payModal?.payouts.map((p) => (
              <div key={p.id} className="flex items-center justify-between border-b border-slate-100 px-3 py-2 last:border-0 dark:border-slate-700">
                <span className="text-sm text-slate-700 dark:text-slate-300">{p.providerName ?? p.providerId.slice(0, 8)}</span>
                <span className="font-semibold text-slate-900 dark:text-slate-100">
                  {displayInDefault(p.amount, p.currency)}
                </span>
              </div>
            ))}
          </div>
          <div className="flex justify-between rounded bg-slate-50 px-3 py-2 dark:bg-slate-800">
            <span className="text-sm font-medium text-slate-600 dark:text-slate-300">{t('payoutsPage.payModalTotal')}</span>
            <span className="font-bold text-slate-900 dark:text-slate-100">
              {payModal ? displayInDefault(
                payModal.payouts.reduce((sum, p) => sum + p.amount, 0),
                payModal.payouts[0]?.currency ?? 'USD',
              ) : '—'}
            </span>
          </div>
        </div>
      </Modal>

      {/* Pay result */}
      <Modal
        open={!!payResult}
        onClose={() => { setPayResult(null); setPayModal(null) }}
        title={payResult?.failed?.length === 0 ? t('payoutsPage.payModalSuccess') : t('payoutsPage.payModalPartialFail')}
        size="sm"
        footer={
          <button type="button" onClick={() => { setPayResult(null); setPayModal(null) }} className="dashboard-btn-primary">
            {t('ui.close')}
          </button>
        }
      >
        <p className="text-sm text-slate-600 dark:text-slate-300">
          {t('ui.success')}: {payResult?.processed ?? 0} &nbsp;|&nbsp; {t('payoutsPage.statusFailed')}: {payResult?.failed?.length ?? 0}
        </p>
      </Modal>

      {/* ── Manual payment modal ─────────────────────────────────────── */}
      <Modal
        open={manualModal}
        onClose={closeManualModal}
        title={t('payoutsPage.manualModalTitle')}
        size="md"
        footer={
          <>
            <button type="button" onClick={closeManualModal} className="dashboard-btn-secondary">
              {t('ui.cancel')}
            </button>
            <button
              type="button"
              onClick={submitManual}
              disabled={manualLoading || !manualProvider.trim() || !manualAmount}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {manualLoading ? t('ui.saving') : t('payoutsPage.manualCreate')}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          {/* Provider name picker */}
          <FormField label={t('payoutsPage.manualProvider')} required>
            <input
              type="search"
              value={manualProviderSearch}
              onChange={(e) => { setManualProviderSearch(e.target.value); setManualProvider('') }}
              placeholder={t('payoutsPage.manualProviderSearch')}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            />
            {providerListLoading ? (
              <p className="mt-1 text-xs text-slate-400">{t('ui.loading')}</p>
            ) : (
              (() => {
                const q = manualProviderSearch.toLowerCase()
                const filtered = q
                  ? providerList.filter((p) => p.name.toLowerCase().includes(q))
                  : providerList
                return filtered.length > 0 ? (
                  <div className="mt-1 max-h-44 overflow-y-auto rounded-lg border border-slate-200 bg-white dark:border-slate-600 dark:bg-slate-800">
                    {filtered.slice(0, 50).map((p) => (
                      <button
                        key={p.id}
                        type="button"
                        onClick={() => { setManualProvider(p.id); setManualProviderSearch(p.name) }}
                        className={[
                          'flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-slate-50 dark:hover:bg-slate-700',
                          manualProvider === p.id ? 'bg-primary/5 font-semibold dark:bg-primary/10' : '',
                        ].join(' ')}
                      >
                        <span className="text-slate-800 dark:text-slate-200">{p.name}</span>
                        {p.type && (
                          <span className="ml-2 shrink-0 rounded bg-slate-100 px-1.5 py-0.5 text-xs capitalize text-slate-500 dark:bg-slate-700 dark:text-slate-400">
                            {p.type.toLowerCase()}
                          </span>
                        )}
                      </button>
                    ))}
                  </div>
                ) : manualProviderSearch ? (
                  <p className="mt-1 text-xs text-slate-400">{t('customerSearchPage.noResults')}</p>
                ) : null
              })()
            )}
            {manualProvider && (
              <p className="mt-1 font-mono text-xs text-slate-400">{manualProvider}</p>
            )}
          </FormField>

          {/* Amount + currency on the same row */}
          <div className="flex gap-3">
            <FormField label={t('payoutsPage.manualAmount')} required>
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={manualAmount}
                onChange={(e) => setManualAmount(e.target.value)}
                placeholder="0.00"
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              />
            </FormField>
            <FormField label={t('payoutsPage.manualCurrency')} required>
              <select
                value={manualCurrency}
                onChange={(e) => setManualCurrency(e.target.value)}
                className="rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              >
                {['USD','EUR','GBP','SAR','AED','KWD','BHD','QAR','OMR','JOD','EGP','TRY'].map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </FormField>
          </div>

          <FormField label={t('payoutsPage.manualMemo')}>
            <textarea
              rows={2}
              value={manualMemo}
              onChange={(e) => setManualMemo(e.target.value)}
              className="modal-textarea w-full"
            />
          </FormField>
          <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
            <input
              type="checkbox"
              checked={manualExecuteNow}
              onChange={(e) => setManualExecuteNow(e.target.checked)}
              className="accent-primary"
            />
            {t('payoutsPage.manualExecuteNow')}
          </label>
        </div>
      </Modal>

      {/* ── Mark as paid modal ───────────────────────────────────────── */}
      <Modal
        open={!!markPaidModal}
        onClose={() => { setMarkPaidModal(null); setMarkPaidTxn(''); setMarkPaidNotes('') }}
        title={t('payoutsPage.markPaidTitle')}
        size="sm"
        footer={
          <>
            <button type="button" onClick={() => setMarkPaidModal(null)} className="dashboard-btn-secondary">
              {t('ui.cancel')}
            </button>
            <button
              type="button"
              onClick={async () => {
                if (!markPaidModal) return
                try {
                  await adminPayoutsAPI.markPaid(markPaidModal.id, { transactionId: markPaidTxn, notes: markPaidNotes })
                  setMarkPaidModal(null)
                  setMarkPaidTxn('')
                  setMarkPaidNotes('')
                  refresh()
                } catch (e) { setError(getApiErrorMessage(e)) }
              }}
              className="dashboard-btn-primary"
            >
              {t('payoutsPage.markPaidConfirm')}
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <FormField label={t('payoutsPage.markPaidTxn')}>
            <input
              type="text"
              value={markPaidTxn}
              onChange={(e) => setMarkPaidTxn(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            />
          </FormField>
          <FormField label={t('payoutsPage.markPaidNotes')}>
            <textarea rows={2} value={markPaidNotes} onChange={(e) => setMarkPaidNotes(e.target.value)} className="modal-textarea w-full" />
          </FormField>
        </div>
      </Modal>

      {/* ── Schedule modal ───────────────────────────────────────────── */}
      <Modal
        open={!!scheduleModal}
        onClose={() => { setScheduleModal(null); setScheduleDate('') }}
        title={t('payoutsPage.scheduleTitle')}
        size="sm"
        footer={
          <>
            <button type="button" onClick={() => setScheduleModal(null)} className="dashboard-btn-secondary">
              {t('ui.cancel')}
            </button>
            <button
              type="button"
              disabled={!scheduleDate}
              onClick={async () => {
                if (!scheduleModal || !scheduleDate) return
                try {
                  await adminPayoutsAPI.schedule(scheduleModal.id, scheduleDate)
                  setScheduleModal(null)
                  setScheduleDate('')
                  refresh()
                } catch (e) { setError(getApiErrorMessage(e)) }
              }}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {t('payoutsPage.scheduleConfirm')}
            </button>
          </>
        }
      >
        <FormField label={t('payoutsPage.scheduleDate')} required>
          <input
            type="datetime-local"
            value={scheduleDate}
            onChange={(e) => setScheduleDate(e.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          />
        </FormField>
      </Modal>

      {/* ── Cancel confirm ───────────────────────────────────────────── */}
      <ConfirmDialog
        open={!!cancelConfirm}
        onClose={() => setCancelConfirm(null)}
        title={t('payoutsPage.cancelConfirmTitle')}
        description={t('payoutsPage.cancelConfirmBody')}
        confirmLabel={t('payoutsPage.confirmCancel')}
        variant="danger"
        onConfirm={async () => {
          if (!cancelConfirm) return
          await adminPayoutsAPI.cancel(cancelConfirm.id)
          setCancelConfirm(null)
          refresh()
        }}
      />
    </>
  )
}
