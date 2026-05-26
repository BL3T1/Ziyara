/**
 * Management > Taxi trips — active list and status updates (taxi-bookings API).
 */

import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, taxiBookingsAPI } from '../../services/api'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'

type TaxiRow = Record<string, unknown> & { id?: string; status?: string }

const TAXI_STATUSES = [
  'SEARCHING',
  'ASSIGNED',
  'EN_ROUTE_TO_PICKUP',
  'ARRIVED_AT_PICKUP',
  'IN_PROGRESS',
  'ARRIVED_AT_DESTINATION',
  'COMPLETED',
  'CANCELLED',
  'NO_DRIVER_FOUND',
] as const

export function TaxiTripsPage() {
  const { t } = useLanguage()
  const [rows, setRows] = useState<TaxiRow[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [detail, setDetail] = useState<TaxiRow | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [statusDraft, setStatusDraft] = useState('')
  const [saving, setSaving] = useState(false)

  const load = () => {
    setLoading(true)
    setError(null)
    taxiBookingsAPI
      .listActive()
      .then((r) => {
        const data = r.data
        setRows(Array.isArray(data) ? (data as TaxiRow[]) : [])
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

  const openDetail = async (id: string) => {
    setDetail(null)
    setStatusDraft('')
    setDetailLoading(true)
    try {
      const r = await taxiBookingsAPI.get(id)
      const row = (r.data as TaxiRow) ?? {}
      setDetail(row)
      setStatusDraft(typeof row.status === 'string' ? row.status : '')
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setDetailLoading(false)
    }
  }

  const saveStatus = async () => {
    if (!detail?.id || !statusDraft) return
    setSaving(true)
    setError(null)
    try {
      await taxiBookingsAPI.updateStatus(detail.id, statusDraft)
      setDetail(null)
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  const str = (v: unknown) => (v != null && String(v).trim() !== '' ? String(v) : '—')

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('taxiTripsPage.title')}</h1>
        </div>
        <button
          type="button"
          onClick={() => load()}
          className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-200"
        >
          {t('taxiTripsPage.refresh')}
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
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('taxiTripsPage.empty')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('taxiTripsPage.colStatus')}</th>
                <th className="px-4 py-3.5">{t('taxiTripsPage.colPickup')}</th>
                <th className="px-4 py-3.5">{t('taxiTripsPage.colDestination')}</th>
                <th className="px-4 py-3.5">{t('taxiTripsPage.colDriver')}</th>
                <th className="px-4 py-3.5 text-end">{t('taxiTripsPage.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={String(r.id)}>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-700 dark:text-slate-200">
                    {str(r.status)}
                  </td>
                  <td className="max-w-[10rem] truncate px-4 py-3 text-sm text-slate-600 dark:text-slate-300" title={str(r.pickupLocation)}>
                    {str(r.pickupLocation)}
                  </td>
                  <td className="max-w-[10rem] truncate px-4 py-3 text-sm text-slate-600 dark:text-slate-300" title={str(r.destinationLocation)}>
                    {str(r.destinationLocation)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {str(r.driverName)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right">
                    {r.id && (
                      <button
                        type="button"
                        onClick={() => openDetail(r.id as string)}
                        className="text-primary hover:underline"
                      >
                        {t('taxiTripsPage.detail')}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <Modal
        open={detail !== null}
        onClose={() => !saving && setDetail(null)}
        title={t('taxiTripsPage.detailTitle')}
        size="sm"
        footer={
          !detailLoading && detail ? (
            <>
              <button
                type="button"
                onClick={() => setDetail(null)}
                disabled={saving}
                className="dashboard-btn-secondary"
              >
                {t('ui.cancel')}
              </button>
              <button
                type="submit"
                form="taxi-status-form"
                disabled={saving || !statusDraft}
                className="dashboard-btn-primary disabled:opacity-50"
              >
                {t('taxiTripsPage.saveStatus')}
              </button>
            </>
          ) : undefined
        }
      >
        {detailLoading ? (
          <p className="py-4 text-center text-sm text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
        ) : detail ? (
          <form id="taxi-status-form" onSubmit={(e) => { e.preventDefault(); saveStatus() }} className="space-y-4">
            <dl className="space-y-2 rounded-xl border border-slate-100 p-3.5 text-sm dark:border-white/[0.05]">
              <div className="flex justify-between gap-4">
                <dt className="text-slate-500 dark:text-slate-400">{t('taxiTripsPage.fBooking')}</dt>
                <dd className="font-medium text-slate-900 dark:text-slate-100">
                  {typeof detail.bookingReference === 'string' && detail.bookingReference.trim()
                    ? detail.bookingReference.trim()
                    : detail.bookingId
                      ? t('paymentsPage.linkedBooking')
                      : t('ui.emDash')}
                </dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-slate-500 dark:text-slate-400">{t('taxiTripsPage.fPlate')}</dt>
                <dd className="font-medium text-slate-900 dark:text-slate-100">{str(detail.licensePlate)}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-slate-500 dark:text-slate-400">{t('taxiTripsPage.fModel')}</dt>
                <dd className="font-medium text-slate-900 dark:text-slate-100">{str(detail.vehicleModel)}</dd>
              </div>
              <div className="flex flex-col gap-1">
                <dt className="text-slate-500 dark:text-slate-400">{t('taxiTripsPage.fPickup')}</dt>
                <dd className="font-medium text-slate-900 dark:text-slate-100">{str(detail.pickupLocation)}</dd>
              </div>
              <div className="flex flex-col gap-1">
                <dt className="text-slate-500 dark:text-slate-400">{t('taxiTripsPage.fDestination')}</dt>
                <dd className="font-medium text-slate-900 dark:text-slate-100">{str(detail.destinationLocation)}</dd>
              </div>
            </dl>
            <FormField label={t('taxiTripsPage.newStatus')} required>
              <select
                value={statusDraft}
                onChange={(e) => setStatusDraft(e.target.value)}
                className="modal-select"
                required
              >
                <option value="">{t('taxiTripsPage.selectStatus')}</option>
                {TAXI_STATUSES.map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
            </FormField>
          </form>
        ) : null}
      </Modal>
    </>
  )
}
