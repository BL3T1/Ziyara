import { useMemo, useState } from 'react'
import type { ServiceEntity } from '../../pages/services/serviceModel'
import { useLanguage } from '../../context/LanguageContext'

const TAXI_TYPES = ['Economy', 'VIP', 'Van'] as const
const BOOKED_ROOM_INDICES = new Set([2, 5, 8, 9, 15])

interface LandingServiceBookingPanelProps {
  service: ServiceEntity
}

export function LandingServiceBookingPanel({ service }: LandingServiceBookingPanelProps) {
  const { t } = useLanguage()
  const [roomPhase, setRoomPhase] = useState(false)
  const [selectedRoom, setSelectedRoom] = useState<number | null>(null)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [taxiType, setTaxiType] = useState<(typeof TAXI_TYPES)[number]>('Economy')

  const isStay = service.category === 'hotels' || service.category === 'resorts'
  const isTaxi = service.category === 'taxis'
  const basePrice = service.price ?? 0

  const taxiEstimate = useMemo(() => {
    if (!isTaxi || !from.trim() || !to.trim()) return 0
    const mult = taxiType === 'VIP' ? 1.45 : taxiType === 'Van' ? 1.25 : 1
    const route = Math.max(basePrice, 12) * mult
    return Math.round(route)
  }, [isTaxi, from, to, taxiType, basePrice])

  const signInContinue = () => {
    const nextPath = `/${service.category}/${service.id}`
    window.location.href = `/login?next=${encodeURIComponent(nextPath)}`
  }

  const priceLine =
    service.price != null ? `${service.currency} ${service.price}` : t('landingBooking.priceOnRequest')

  const canConfirmStay = selectedRoom != null
  const canConfirmTaxi = taxiEstimate > 0

  return (
    <div className="mt-8">
      {isStay && roomPhase ? (
        <div className="mb-4 rounded-[20px] border p-5" style={{ borderColor: 'rgba(90, 122, 130, 0.2)', background: 'rgba(255, 255, 255, 0.92)' }}>
          <h2 className="text-lg font-semibold" style={{ color: 'var(--ink-heading)' }}>
            {t('landingBooking.chooseRoomTitle')}
          </h2>
          <p className="mt-1 text-sm" style={{ color: 'var(--ink-muted)' }}>
            {t('landingBooking.roomLegend')}
          </p>
          <div className="mt-4 grid grid-cols-5 gap-2 sm:grid-cols-10">
            {Array.from({ length: 20 }, (_, i) => {
              const n = i + 1
              const booked = BOOKED_ROOM_INDICES.has(i)
              const selected = selectedRoom === n
              return (
                <button
                  key={n}
                  type="button"
                  disabled={booked}
                  onClick={() => setSelectedRoom(n)}
                  className="rounded-lg border-2 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50"
                  style={{
                    borderColor: booked ? '#c45c5c' : selected ? 'var(--accent-teal)' : 'rgba(90, 122, 130, 0.25)',
                    background: booked ? 'rgba(196, 92, 92, 0.12)' : selected ? 'rgba(90, 160, 150, 0.12)' : 'transparent',
                    color: booked ? '#8b3a3a' : 'var(--ink-heading)',
                  }}
                >
                  {t('landingBooking.roomLabel')} {n}
                </button>
              )
            })}
          </div>
        </div>
      ) : null}

      {isTaxi ? (
        <div className="mb-4 rounded-[20px] border p-5" style={{ borderColor: 'rgba(90, 122, 130, 0.2)', background: 'rgba(255, 255, 255, 0.92)' }}>
          <h2 className="text-lg font-semibold" style={{ color: 'var(--ink-heading)' }}>
            {t('landingBooking.ridePlanTitle')}
          </h2>
          <label className="mt-4 block">
            <span className="text-xs font-medium uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
              {t('landingBooking.fromLabel')}
            </span>
            <input
              value={from}
              onChange={(e) => setFrom(e.target.value)}
              className="mt-1 w-full rounded-xl border px-3 py-2.5 text-sm outline-none focus:ring-2"
              style={{ borderColor: 'rgba(90, 122, 130, 0.25)', color: 'var(--ink-heading)' }}
              placeholder={t('landingBooking.fromPlaceholder')}
            />
          </label>
          <label className="mt-3 block">
            <span className="text-xs font-medium uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
              {t('landingBooking.toLabel')}
            </span>
            <input
              value={to}
              onChange={(e) => setTo(e.target.value)}
              className="mt-1 w-full rounded-xl border px-3 py-2.5 text-sm outline-none focus:ring-2"
              style={{ borderColor: 'rgba(90, 122, 130, 0.25)', color: 'var(--ink-heading)' }}
              placeholder={t('landingBooking.toPlaceholder')}
            />
          </label>
          <p className="mt-4 text-sm font-medium" style={{ color: 'var(--ink-heading)' }}>
            {t('landingBooking.vehicleType')}
          </p>
          <div className="mt-2 flex flex-wrap gap-2">
            {TAXI_TYPES.map((type) => {
              const sel = taxiType === type
              return (
                <button
                  key={type}
                  type="button"
                  onClick={() => setTaxiType(type)}
                  className="rounded-xl border-2 px-4 py-2 text-sm font-semibold transition-colors"
                  style={{
                    borderColor: sel ? 'var(--accent-teal)' : 'rgba(90, 122, 130, 0.25)',
                    background: sel ? 'rgba(90, 160, 150, 0.15)' : 'transparent',
                    color: 'var(--ink-heading)',
                  }}
                >
                  {type}
                </button>
              )
            })}
          </div>
        </div>
      ) : null}

      <div
        className="fixed inset-x-0 bottom-0 z-40 border-t px-4 py-4 shadow-[0_-8px_24px_rgba(15,23,42,0.08)] sm:px-8"
        style={{ background: 'rgba(255, 255, 255, 0.96)', borderColor: 'rgba(90, 122, 130, 0.15)' }}
      >
        <div className="mx-auto flex max-w-3xl flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <p className="text-xs font-medium uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
              {isStay ? t('landingBooking.pricePerNight') : isTaxi ? t('landingBooking.estimateLabel') : t('landingBooking.startingFrom')}
            </p>
            <p className="truncate text-xl font-bold" style={{ color: 'var(--ink-heading)' }}>
              {isTaxi ? (canConfirmTaxi ? `${service.currency} ${taxiEstimate}` : '—') : priceLine}
            </p>
          </div>
          <div className="flex shrink-0 flex-col gap-2 sm:flex-row">
            {isStay && !roomPhase ? (
              <button type="button" onClick={() => setRoomPhase(true)} className="lp-btn lp-btn-primary px-6 py-3 text-center">
                {t('landingBooking.selectRoom')}
              </button>
            ) : null}
            {isStay && roomPhase ? (
              <button
                type="button"
                disabled={!canConfirmStay}
                onClick={signInContinue}
                className="lp-btn lp-btn-primary px-6 py-3 text-center disabled:cursor-not-allowed disabled:opacity-50"
              >
                {t('landingBooking.confirmContinue')}
              </button>
            ) : null}
            {!isStay && !isTaxi ? (
              <button type="button" onClick={signInContinue} className="lp-btn lp-btn-primary px-6 py-3 text-center">
                {t('landingBooking.bookNow')}
              </button>
            ) : null}
            {isTaxi ? (
              <button
                type="button"
                disabled={!canConfirmTaxi}
                onClick={signInContinue}
                className="lp-btn lp-btn-primary px-6 py-3 text-center disabled:cursor-not-allowed disabled:opacity-50"
              >
                {t('landingBooking.requestRide')}
              </button>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  )
}
