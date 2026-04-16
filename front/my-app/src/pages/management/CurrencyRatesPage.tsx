/**
 * Management > Currency rates — list, create, delete (finance roles on backend).
 */

import { useEffect, useState } from 'react'
import { useDisplayCurrency } from '../../context/DisplayCurrencyContext'
import { useLanguage } from '../../context/LanguageContext'
import { currencyAPI, getApiErrorMessage } from '../../services/api'
import type { ExchangeRateRowDto } from '../../types/api'

export function CurrencyRatesPage() {
  const { t } = useLanguage()
  const { refreshDisplayCurrency } = useDisplayCurrency()
  const [rows, setRows] = useState<ExchangeRateRowDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [detailId, setDetailId] = useState<string | null>(null)
  const [detailRow, setDetailRow] = useState<ExchangeRateRowDto | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [form, setForm] = useState({ fromCurrency: '', toCurrency: '', rate: '', effectiveDate: '' })
  const [submitting, setSubmitting] = useState(false)
  const [detailEditing, setDetailEditing] = useState(false)
  const [editRate, setEditRate] = useState('')
  const [editEffectiveDate, setEditEffectiveDate] = useState('')

  const load = () => {
    setLoading(true)
    setError(null)
    currencyAPI
      .listRates()
      .then((r) => {
        const data = r.data
        setRows(Array.isArray(data) ? (data as ExchangeRateRowDto[]) : [])
      })
      .catch((e) => {
        setError(getApiErrorMessage(e))
        setRows([])
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    const fromCurrency = form.fromCurrency.trim().toUpperCase()
    const toCurrency = form.toCurrency.trim().toUpperCase()
    const rateNum = Number(form.rate)
    if (!fromCurrency || !toCurrency || !Number.isFinite(rateNum) || rateNum <= 0) return
    setSubmitting(true)
    setError(null)
    try {
      const body: Record<string, unknown> = {
        fromCurrency,
        toCurrency,
        rate: rateNum,
      }
      if (form.effectiveDate.trim()) body.effectiveDate = form.effectiveDate.trim()
      await currencyAPI.createRate(body)
      setShowCreate(false)
      setForm({ fromCurrency: '', toCurrency: '', rate: '', effectiveDate: '' })
      refreshDisplayCurrency()
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setSubmitting(false)
    }
  }

  const openDetail = async (id: string) => {
    setDetailId(id)
    setDetailEditing(false)
    setDetailRow(null)
    setDetailLoading(true)
    setError(null)
    try {
      const res = await currencyAPI.getRate(id)
      setDetailRow((res.data ?? null) as ExchangeRateRowDto | null)
    } catch (e) {
      setError(getApiErrorMessage(e))
      setDetailId(null)
    } finally {
      setDetailLoading(false)
    }
  }

  const closeDetail = () => {
    setDetailId(null)
    setDetailRow(null)
    setDetailLoading(false)
    setDetailEditing(false)
    setEditRate('')
    setEditEffectiveDate('')
  }

  useEffect(() => {
    if (!detailRow) return
    const r = detailRow.rate
    const n = typeof r === 'number' ? r : Number(r)
    setEditRate(Number.isFinite(n) ? String(n) : '')
    setEditEffectiveDate(detailRow.effectiveDate?.slice(0, 10) ?? '')
  }, [detailRow])

  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!detailId || !detailRow) return
    const rateNum = Number(editRate)
    if (!Number.isFinite(rateNum) || rateNum <= 0) return
    setSubmitting(true)
    setError(null)
    try {
      const body: Record<string, unknown> = { rate: rateNum }
      if (editEffectiveDate.trim()) body.effectiveDate = editEffectiveDate.trim()
      await currencyAPI.updateRate(detailId, body)
      refreshDisplayCurrency()
      setDetailEditing(false)
      const res = await currencyAPI.getRate(detailId)
      setDetailRow((res.data ?? null) as ExchangeRateRowDto | null)
      load()
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id: string) => {
    if (!window.confirm(t('currencyRatesPage.confirmDelete'))) return
    setDeletingId(id)
    setError(null)
    try {
      await currencyAPI.deleteRate(id)
      refreshDisplayCurrency()
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setDeletingId(null)
    }
  }

  const fmtRate = (r: unknown) => {
    if (r == null) return '—'
    const n = typeof r === 'number' ? r : Number(r)
    return Number.isFinite(n) ? String(n) : String(r)
  }

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('currencyRatesPage.title')}</h1>
        </div>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="dashboard-btn-primary shrink-0"
        >
          {t('currencyRatesPage.addRate')}
        </button>
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="mt-6 table-shell">
        {loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : rows.length === 0 ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('currencyRatesPage.empty')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('currencyRatesPage.colFrom')}</th>
                <th className="px-4 py-3.5">{t('currencyRatesPage.colTo')}</th>
                <th className="px-4 py-3.5">{t('currencyRatesPage.colRate')}</th>
                <th className="px-4 py-3.5">{t('currencyRatesPage.colEffective')}</th>
                <th className="px-4 py-3.5 text-end">{t('currencyRatesPage.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td className="px-4 py-3 text-sm font-medium text-slate-900 dark:text-slate-100">
                    {row.fromCurrency ?? '—'}
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-700 dark:text-slate-200">{row.toCurrency ?? '—'}</td>
                  <td className="px-4 py-3 font-mono text-sm text-slate-700 dark:text-slate-200">
                    {fmtRate(row.rate)}
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {row.effectiveDate ?? '—'}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex flex-wrap items-center justify-end gap-3">
                      <button
                        type="button"
                        disabled={detailLoading && detailId === row.id}
                        onClick={() => openDetail(row.id)}
                        className="text-sm text-primary hover:underline disabled:opacity-50"
                      >
                        {t('currencyRatesPage.viewDetail')}
                      </button>
                      <button
                        type="button"
                        disabled={deletingId === row.id}
                        onClick={() => handleDelete(row.id)}
                        className="text-sm text-red-600 hover:underline disabled:opacity-50 dark:text-red-400"
                      >
                        {t('currencyRatesPage.delete')}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {detailId && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          onClick={() => !detailLoading && !submitting && closeDetail()}
        >
          <div
            className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-700 dark:bg-slate-800"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">
              {detailEditing ? t('currencyRatesPage.editTitle') : t('currencyRatesPage.detailTitle')}
            </h3>
            {detailLoading ? (
              <p className="mt-4 text-sm text-slate-600 dark:text-slate-300">{t('ui.loading')}</p>
            ) : detailRow && detailEditing ? (
              <form onSubmit={handleSaveEdit} className="mt-4 space-y-3">
                <div className="flex justify-between gap-4 text-sm">
                  <span className="text-slate-500 dark:text-slate-400">{t('currencyRatesPage.colFrom')}</span>
                  <span className="font-medium text-slate-900 dark:text-slate-100">{detailRow.fromCurrency ?? '—'}</span>
                </div>
                <div className="flex justify-between gap-4 text-sm">
                  <span className="text-slate-500 dark:text-slate-400">{t('currencyRatesPage.colTo')}</span>
                  <span className="font-medium text-slate-900 dark:text-slate-100">{detailRow.toCurrency ?? '—'}</span>
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">{t('currencyRatesPage.rate')}</label>
                  <input
                    type="number"
                    step="any"
                    min="0"
                    value={editRate}
                    onChange={(e) => setEditRate(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 font-mono dark:border-slate-600 dark:bg-slate-700"
                    required
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">{t('currencyRatesPage.effectiveDate')}</label>
                  <input
                    type="date"
                    value={editEffectiveDate}
                    onChange={(e) => setEditEffectiveDate(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-700"
                  />
                </div>
                <div className="flex gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setDetailEditing(false)}
                    disabled={submitting}
                    className="dashboard-btn-secondary flex-1"
                  >
                    {t('ui.cancel')}
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    className="dashboard-btn-primary flex-1 disabled:opacity-50"
                  >
                    {t('currencyRatesPage.save')}
                  </button>
                </div>
              </form>
            ) : detailRow ? (
              <>
                <dl className="mt-4 space-y-2 text-sm">
                  <div className="flex justify-between gap-4">
                    <dt className="text-slate-500 dark:text-slate-400">{t('currencyRatesPage.colFrom')}</dt>
                    <dd className="font-medium text-slate-900 dark:text-slate-100">{detailRow.fromCurrency ?? '—'}</dd>
                  </div>
                  <div className="flex justify-between gap-4">
                    <dt className="text-slate-500 dark:text-slate-400">{t('currencyRatesPage.colTo')}</dt>
                    <dd className="font-medium text-slate-900 dark:text-slate-100">{detailRow.toCurrency ?? '—'}</dd>
                  </div>
                  <div className="flex justify-between gap-4">
                    <dt className="text-slate-500 dark:text-slate-400">{t('currencyRatesPage.colRate')}</dt>
                    <dd className="font-mono text-slate-900 dark:text-slate-100">{fmtRate(detailRow.rate)}</dd>
                  </div>
                  <div className="flex justify-between gap-4">
                    <dt className="text-slate-500 dark:text-slate-400">{t('currencyRatesPage.colEffective')}</dt>
                    <dd className="text-slate-900 dark:text-slate-100">{detailRow.effectiveDate ?? '—'}</dd>
                  </div>
                </dl>
                <div className="mt-6 flex flex-col gap-2 sm:flex-row">
                  <button
                    type="button"
                    onClick={() => setDetailEditing(true)}
                    className="dashboard-btn-primary flex-1"
                  >
                    {t('currencyRatesPage.editButton')}
                  </button>
                  <button
                    type="button"
                    onClick={closeDetail}
                    className="w-full rounded-lg border border-slate-300 px-4 py-2 text-sm dark:border-slate-600 sm:flex-1"
                  >
                    {t('ui.close')}
                  </button>
                </div>
              </>
            ) : null}
          </div>
        </div>
      )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={() => !submitting && setShowCreate(false)}>
          <div
            className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-700 dark:bg-slate-800"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">{t('currencyRatesPage.createTitle')}</h3>
            <form onSubmit={handleCreate} className="mt-4 space-y-3">
              <div>
                <label className="mb-1 block text-sm font-medium">{t('currencyRatesPage.from')}</label>
                <input
                  value={form.fromCurrency}
                  onChange={(e) => setForm((f) => ({ ...f, fromCurrency: e.target.value }))}
                  maxLength={3}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 uppercase dark:border-slate-600 dark:bg-slate-700"
                  placeholder="USD"
                  required
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium">{t('currencyRatesPage.to')}</label>
                <input
                  value={form.toCurrency}
                  onChange={(e) => setForm((f) => ({ ...f, toCurrency: e.target.value }))}
                  maxLength={3}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 uppercase dark:border-slate-600 dark:bg-slate-700"
                  placeholder="SAR"
                  required
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium">{t('currencyRatesPage.rate')}</label>
                <input
                  type="number"
                  step="any"
                  min="0"
                  value={form.rate}
                  onChange={(e) => setForm((f) => ({ ...f, rate: e.target.value }))}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-700"
                  required
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium">{t('currencyRatesPage.effectiveDate')}</label>
                <input
                  type="date"
                  value={form.effectiveDate}
                  onChange={(e) => setForm((f) => ({ ...f, effectiveDate: e.target.value }))}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-700"
                />
              </div>
              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowCreate(false)}
                  disabled={submitting}
                  className="dashboard-btn-secondary flex-1"
                >
                  {t('ui.cancel')}
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="dashboard-btn-primary flex-1 disabled:opacity-50"
                >
                  {t('currencyRatesPage.save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  )
}
