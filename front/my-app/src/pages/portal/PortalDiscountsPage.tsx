import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { getApiErrorMessage, portalAPI } from '../../services/api'
import type { DiscountBalanceDto, DiscountDto, CreatePortalDiscountPayload, DiscountTypeDto } from '../../types/api'
import { Card } from '../../components/Card'
import { Modal } from '../../components/Modal'
import { StatusBadge } from '../../components/StatusBadge'

// ── Create-discount modal ─────────────────────────────────────────────────────

function CreateDiscountModal({
  balance,
  onClose,
  onCreated,
}: {
  balance: DiscountBalanceDto
  onClose: () => void
  onCreated: (d: DiscountDto) => void
}) {
  const { t } = useLanguage()
  const [code, setCode] = useState('')
  const [type, setType] = useState<DiscountTypeDto>('PERCENTAGE')
  const [value, setValue] = useState('')
  const [description, setDescription] = useState('')
  const [endDate, setEndDate] = useState('')
  const [usageLimit, setUsageLimit] = useState('0')
  const [minBooking, setMinBooking] = useState('')
  const [maxDiscount, setMaxDiscount] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const parsedValue = parseFloat(value)
  const afterCreate = !isNaN(parsedValue)
    ? (balance.availableAmount - parsedValue).toFixed(2)
    : null

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    if (!code.trim()) { setError(t('portalPages.discountValidationCode')); return }
    if (isNaN(parsedValue) || parsedValue <= 0) { setError(t('portalPages.discountValidationValue')); return }
    if (!endDate) { setError(t('portalPages.discountValidationExpiry')); return }
    if (parsedValue > balance.availableAmount) {
      setError(t('portalPages.discountInsufficientBalance'))
      return
    }

    const payload: CreatePortalDiscountPayload = {
      code: code.trim().toUpperCase(),
      type,
      value: parsedValue,
      description: description.trim() || undefined,
      endDate: new Date(endDate).toISOString(),
      usageLimit: parseInt(usageLimit) || 0,
      minBookingAmount: minBooking ? parseFloat(minBooking) : undefined,
      maxDiscountAmount: maxDiscount ? parseFloat(maxDiscount) : undefined,
    }

    setSubmitting(true)
    try {
      const res = await portalAPI.createDiscount(payload)
      onCreated(res.data as DiscountDto)
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  const inputCls =
    'mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100'

  return (
    <Modal open onClose={onClose} title={t('portalPages.discountModalTitle')}>
      <form onSubmit={handleSubmit} className="space-y-4 p-1">
        {error && (
          <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-950/40 dark:text-red-300">
            {error}
          </p>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
              {t('portalPages.discountFieldCode')}
            </label>
            <input
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase())}
              className={inputCls}
              placeholder="e.g. SUMMER20"
              maxLength={50}
            />
            <p className="mt-1 text-xs text-slate-400">{t('portalPages.discountCodeHint')}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
              {t('portalPages.discountFieldType')}
            </label>
            <select value={type} onChange={(e) => setType(e.target.value as DiscountTypeDto)} className={inputCls}>
              <option value="PERCENTAGE">{t('portalPages.discountTypePercentage')}</option>
              <option value="FIXED_AMOUNT">{t('portalPages.discountTypeFixed')}</option>
            </select>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('portalPages.discountFieldValue')}
          </label>
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            className={inputCls}
          />
          <p className="mt-1 text-xs text-slate-400">{t('portalPages.discountValueHint')}</p>
          {afterCreate !== null && (
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
              {t('portalPages.discountBalanceDebit')}{' '}
              <span className={parseFloat(afterCreate) < 0 ? 'text-red-500' : 'font-medium'}>
                {afterCreate} {balance.currency}
              </span>
            </p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('portalPages.discountFieldDescription')}
          </label>
          <input value={description} onChange={(e) => setDescription(e.target.value)} className={inputCls} />
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
              {t('portalPages.discountFieldEndDate')}
            </label>
            <input
              type="datetime-local"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className={inputCls}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
              {t('portalPages.discountFieldUsageLimit')}
            </label>
            <input
              type="number"
              min="0"
              step="1"
              value={usageLimit}
              onChange={(e) => setUsageLimit(e.target.value)}
              className={inputCls}
            />
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
              {t('portalPages.discountFieldMinBooking')}
            </label>
            <input
              type="number"
              min="0"
              step="0.01"
              value={minBooking}
              onChange={(e) => setMinBooking(e.target.value)}
              className={inputCls}
            />
          </div>
          {type === 'PERCENTAGE' && (
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
                {t('portalPages.discountFieldMaxDiscount')}
              </label>
              <input
                type="number"
                min="0"
                step="0.01"
                value={maxDiscount}
                onChange={(e) => setMaxDiscount(e.target.value)}
                className={inputCls}
              />
            </div>
          )}
        </div>

        <div className="flex gap-3 pt-1">
          <button type="submit" disabled={submitting} className="dashboard-btn-primary disabled:opacity-50">
            {submitting ? t('portalPages.saving') : t('portalPages.save')}
          </button>
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-200"
          >
            {t('ui.cancel')}
          </button>
        </div>
      </form>
    </Modal>
  )
}

// ── Balance meter ─────────────────────────────────────────────────────────────

function BalanceMeter({ balance }: { balance: DiscountBalanceDto }) {
  const { t } = useLanguage()
  const pct =
    balance.allocatedAmount > 0
      ? Math.min(100, (balance.spentAmount / balance.allocatedAmount) * 100)
      : 0

  return (
    <Card className="p-5">
      <p className="mb-3 text-sm font-semibold text-slate-700 dark:text-slate-200">
        {t('portalPages.discountBalance')}
      </p>
      <div className="mb-3 h-3 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700">
        <div
          role="progressbar"
          aria-valuenow={Math.round(pct)}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={t('portalPages.discountBalance')}
          className="h-full rounded-full bg-primary transition-all duration-500"
          style={{ width: `${pct}%` }}
        />
      </div>
      <div className="grid grid-cols-3 gap-2 text-center text-sm">
        <div>
          <p className="font-semibold text-slate-800 dark:text-slate-100">
            {balance.allocatedAmount.toFixed(2)}
          </p>
          <p className="text-xs text-slate-400">{t('portalPages.discountAllocated')}</p>
        </div>
        <div>
          <p className="font-semibold text-slate-800 dark:text-slate-100">
            {balance.spentAmount.toFixed(2)}
          </p>
          <p className="text-xs text-slate-400">{t('portalPages.discountSpent')}</p>
        </div>
        <div>
          <p className="font-semibold text-primary">
            {balance.availableAmount.toFixed(2)} {balance.currency}
          </p>
          <p className="text-xs text-slate-400">{t('portalPages.discountAvailable')}</p>
        </div>
      </div>
    </Card>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

const PAGE_SIZE = 20

export function PortalDiscountsPage() {
  const { t } = useLanguage()
  useDocumentMeta({ title: `${t('nav.discounts')} — Ziyara` })
  const [balance, setBalance] = useState<DiscountBalanceDto | null>(null)
  const [discounts, setDiscounts] = useState<DiscountDto[]>([])
  const [discountPage, setDiscountPage] = useState(0)
  const [discountTotal, setDiscountTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)
  const [deactivating, setDeactivating] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([portalAPI.getDiscountBalance(), portalAPI.listDiscounts({ page: 0, size: PAGE_SIZE })])
      .then(([balRes, listRes]) => {
        setBalance(balRes.data as DiscountBalanceDto)
        const paged = listRes.data as { content: DiscountDto[]; totalElements: number }
        setDiscounts(paged.content ?? [])
        setDiscountTotal(paged.totalElements ?? 0)
        setDiscountPage(0)
      })
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  function loadMoreDiscounts() {
    const nextPage = discountPage + 1
    setLoadingMore(true)
    portalAPI.listDiscounts({ page: nextPage, size: PAGE_SIZE })
      .then((res) => {
        const paged = res.data as { content: DiscountDto[]; totalElements: number }
        setDiscounts((prev) => [...prev, ...(paged.content ?? [])])
        setDiscountTotal(paged.totalElements ?? 0)
        setDiscountPage(nextPage)
      })
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoadingMore(false))
  }

  function handleCreated(d: DiscountDto) {
    setDiscounts((prev) => [d, ...prev])
    if (balance) {
      const newVal = parseFloat(String(d.value ?? 0))
      setBalance({ ...balance, spentAmount: balance.spentAmount + newVal, availableAmount: balance.availableAmount - newVal })
    }
    setShowCreate(false)
    setSuccessMsg(t('portalPages.discountPendingApproval'))
    setTimeout(() => setSuccessMsg(null), 8000)
  }

  async function handleDeactivate(id: string) {
    setDeactivating(id)
    try {
      await portalAPI.deactivateDiscount(id)
      setDiscounts((prev) => prev.map((d) => (d.id === id ? { ...d, status: 'INACTIVE' } : d)))
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setDeactivating(null)
    }
  }

  function fmtDate(s?: string) {
    if (!s) return '—'
    return new Date(s).toLocaleDateString()
  }


  if (loading) return <p className="text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>

  return (
    <>
      <div className="mb-6">
        <h1 className="app-page-title">{t('nav.discounts')}</h1>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t('portalPages.discountsIntro')}</p>
      </div>

      <div role="status" aria-live="polite" aria-atomic="true">
        {error && (
          <p className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
            {error}
          </p>
        )}
        {successMsg && (
          <p className="mb-4 rounded-lg border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-800 dark:border-green-800 dark:bg-green-950/40 dark:text-green-200">
            {successMsg}
          </p>
        )}
      </div>

      {balance && <BalanceMeter balance={balance} />}

      {balance && balance.allocatedAmount <= 0 && (
        <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">{t('portalPages.discountNoBalance')}</p>
      )}

      <div className="mt-5 flex items-center justify-between">
        <p className="text-sm font-medium text-slate-700 dark:text-slate-200">{t('nav.discounts')}</p>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          disabled={!balance || balance.availableAmount <= 0}
          className="dashboard-btn-primary disabled:cursor-not-allowed disabled:opacity-40"
        >
          + {t('portalPages.discountCreate')}
        </button>
      </div>

      <Card className="mt-3 overflow-hidden p-0">
        {discounts.length === 0 ? (
          <p className="p-6 text-sm text-slate-500 dark:text-slate-400">{t('portalPages.noDiscounts')}</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 dark:border-slate-700 dark:bg-slate-800/50">
                  {[
                    t('portalPages.discountColCode'),
                    t('portalPages.discountColType'),
                    t('portalPages.discountColValue'),
                    t('portalPages.discountColStatus'),
                    t('portalPages.discountColUsage'),
                    t('portalPages.discountColExpires'),
                    t('portalPages.discountColActions'),
                  ].map((h) => (
                    <th
                      key={h}
                      className="whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400"
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-700/60">
                {discounts.map((d) => {
                  const valueDisplay =
                    d.type === 'PERCENTAGE'
                      ? `${d.value}%`
                      : `${d.value ?? '—'} ${balance?.currency ?? ''}`
                  const usageDisplay =
                    d.usageLimit && d.usageLimit > 0
                      ? `${d.usageCount ?? 0} / ${d.usageLimit}`
                      : `${d.usageCount ?? 0} / ∞`
                  const canDeactivate = d.status === 'ACTIVE' || d.status === 'PENDING_APPROVAL'
                  return (
                    <tr key={d.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30">
                      <td className="px-4 py-3 font-mono font-semibold text-slate-900 dark:text-slate-100">
                        {d.code}
                      </td>
                      <td className="px-4 py-3 text-slate-600 dark:text-slate-300">
                        {d.type === 'PERCENTAGE' ? t('portalPages.discountTypePercentage') : t('portalPages.discountTypeFixed')}
                      </td>
                      <td className="px-4 py-3 font-medium text-slate-800 dark:text-slate-200">
                        {valueDisplay}
                      </td>
                      <td className="px-4 py-3"><StatusBadge status={d.status} /></td>
                      <td className="px-4 py-3 text-slate-500 dark:text-slate-400">{usageDisplay}</td>
                      <td className="px-4 py-3 text-slate-500 dark:text-slate-400">{fmtDate(d.endDate)}</td>
                      <td className="px-4 py-3">
                        {canDeactivate && (
                          <button
                            type="button"
                            disabled={deactivating === d.id}
                            onClick={() => handleDeactivate(d.id)}
                            className="text-xs font-medium text-red-600 hover:underline disabled:opacity-50 dark:text-red-400"
                          >
                            {deactivating === d.id ? '…' : t('portalPages.discountDeactivate')}
                          </button>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {discounts.length < discountTotal && (
        <div className="mt-4 flex justify-center">
          <button
            type="button"
            disabled={loadingMore}
            onClick={loadMoreDiscounts}
            className="dashboard-btn-secondary disabled:opacity-50"
          >
            {loadingMore ? t('ui.loading') : t('portalPages.loadMore', { loaded: discounts.length, total: discountTotal })}
          </button>
        </div>
      )}

      {showCreate && balance && (
        <CreateDiscountModal balance={balance} onClose={() => setShowCreate(false)} onCreated={handleCreated} />
      )}
    </>
  )
}
