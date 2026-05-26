/**
 * Management > Currency rates — list, create, delete (finance roles on backend).
 */

import { useEffect, useState } from 'react'
import { useDisplayCurrency } from '../../context/DisplayCurrencyContext'
import { useLanguage } from '../../context/LanguageContext'
import { currencyAPI, getApiErrorMessage } from '../../services/api'
import type { ExchangeRateRowDto } from '../../types/api'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'

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

      {/* ── Detail / Edit modal ───────────────────────────────────────── */}
      <Modal
        open={!!detailId}
        onClose={() => !detailLoading && !submitting && closeDetail()}
        title={detailEditing ? t('currencyRatesPage.editTitle') : t('currencyRatesPage.detailTitle')}
        size="sm"
        footer={
          detailLoading ? undefined : detailRow && detailEditing ? (
            <>
              <button
                type="button"
                onClick={() => setDetailEditing(false)}
                disabled={submitting}
                className="dashboard-btn-secondary"
              >
                {t('ui.cancel')}
              </button>
              <button
                type="submit"
                form="currency-edit-form"
                disabled={submitting}
                className="dashboard-btn-primary disabled:opacity-50"
              >
                {t('currencyRatesPage.save')}
              </button>
            </>
          ) : detailRow ? (
            <>
              <button
                type="button"
                onClick={closeDetail}
                className="dashboard-btn-secondary"
              >
                {t('ui.close')}
              </button>
              <button
                type="button"
                onClick={() => setDetailEditing(true)}
                className="dashboard-btn-primary"
              >
                {t('currencyRatesPage.editButton')}
              </button>
            </>
          ) : undefined
        }
      >
        {detailLoading ? (
          <p className="py-4 text-center text-sm text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
        ) : detailRow && detailEditing ? (
          <form id="currency-edit-form" onSubmit={handleSaveEdit} className="space-y-4">
            <dl className="space-y-2 rounded-xl border border-slate-100 p-3.5 text-sm dark:border-white/[0.05]">
              <div className="flex justify-between gap-4">
                <dt className="text-slate-500 dark:text-slate-400">{t('currencyRatesPage.colFrom')}</dt>
                <dd className="font-medium text-slate-900 dark:text-slate-100">{detailRow.fromCurrency ?? '—'}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-slate-500 dark:text-slate-400">{t('currencyRatesPage.colTo')}</dt>
                <dd className="font-medium text-slate-900 dark:text-slate-100">{detailRow.toCurrency ?? '—'}</dd>
              </div>
            </dl>
            <FormField label={t('currencyRatesPage.rate')} required>
              <input
                type="number"
                step="any"
                min="0"
                value={editRate}
                onChange={(e) => setEditRate(e.target.value)}
                className="modal-input font-mono"
                required
              />
            </FormField>
            <FormField label={t('currencyRatesPage.effectiveDate')}>
              <input
                type="date"
                value={editEffectiveDate}
                onChange={(e) => setEditEffectiveDate(e.target.value)}
                className="modal-input"
              />
            </FormField>
          </form>
        ) : detailRow ? (
          <dl className="space-y-3 text-sm">
            {[
              [t('currencyRatesPage.colFrom'), detailRow.fromCurrency ?? '—', ''],
              [t('currencyRatesPage.colTo'),   detailRow.toCurrency ?? '—',   ''],
              [t('currencyRatesPage.colRate'),  fmtRate(detailRow.rate),       'font-mono'],
              [t('currencyRatesPage.colEffective'), detailRow.effectiveDate ?? '—', ''],
            ].map(([label, value, extra]) => (
              <div key={String(label)} className="flex items-center justify-between gap-4 rounded-lg border border-slate-100 px-3.5 py-2.5 dark:border-white/[0.05]">
                <dt className="text-slate-500 dark:text-slate-400">{label}</dt>
                <dd className={`font-medium text-slate-900 dark:text-slate-100 ${extra}`}>{value}</dd>
              </div>
            ))}
          </dl>
        ) : null}
      </Modal>

      {/* ── Create modal ──────────────────────────────────────────────── */}
      <Modal
        open={showCreate}
        onClose={() => !submitting && setShowCreate(false)}
        title={t('currencyRatesPage.createTitle')}
        size="sm"
        footer={
          <>
            <button
              type="button"
              onClick={() => setShowCreate(false)}
              disabled={submitting}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="currency-create-form"
              disabled={submitting}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {t('currencyRatesPage.save')}
            </button>
          </>
        }
      >
        <form id="currency-create-form" onSubmit={handleCreate} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <FormField label={t('currencyRatesPage.from')} required>
              <input
                value={form.fromCurrency}
                onChange={(e) => setForm((f) => ({ ...f, fromCurrency: e.target.value }))}
                maxLength={3}
                className="modal-input font-mono uppercase tracking-wider"
                placeholder="USD"
                required
              />
            </FormField>
            <FormField label={t('currencyRatesPage.to')} required>
              <input
                value={form.toCurrency}
                onChange={(e) => setForm((f) => ({ ...f, toCurrency: e.target.value }))}
                maxLength={3}
                className="modal-input font-mono uppercase tracking-wider"
                placeholder="SAR"
                required
              />
            </FormField>
          </div>
          <FormField label={t('currencyRatesPage.rate')} required>
            <input
              type="number"
              step="any"
              min="0"
              value={form.rate}
              onChange={(e) => setForm((f) => ({ ...f, rate: e.target.value }))}
              className="modal-input font-mono"
              required
            />
          </FormField>
          <FormField label={t('currencyRatesPage.effectiveDate')}>
            <input
              type="date"
              value={form.effectiveDate}
              onChange={(e) => setForm((f) => ({ ...f, effectiveDate: e.target.value }))}
              className="modal-input"
            />
          </FormField>
        </form>
      </Modal>
    </>
  )
}
