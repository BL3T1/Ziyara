import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { ServiceEntity } from '../../pages/services/serviceModel'
import { useLanguage } from '../../context/LanguageContext'
import { useAuth } from '../../context/AuthContext'

const TAXI_TYPES = ['Economy', 'VIP', 'Van'] as const

const BOOKED_ROOM_INDICES = new Set([2, 5, 8, 9, 15])

interface LandingServiceBookingPanelProps {
  service: ServiceEntity
}

export function LandingServiceBookingPanel({ service }: LandingServiceBookingPanelProps) {
  const { t } = useLanguage()
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()

  const [checkIn, setCheckIn] = useState('')
  const [checkOut, setCheckOut] = useState('')
  const [guests, setGuests] = useState(1)
  const [roomPhase, setRoomPhase] = useState(false)
  const [selectedRoom, setSelectedRoom] = useState<number | null>(null)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [taxiType, setTaxiType] = useState<(typeof TAXI_TYPES)[number]>('Economy')

  const isStay = service.category === 'hotels' || service.category === 'resorts'
  const isTaxi = service.category === 'taxis'
  const basePrice = service.price ?? 0

  const nights = useMemo(() => {
    if (!checkIn || !checkOut) return 0
    const diff = new Date(checkOut).getTime() - new Date(checkIn).getTime()
    return Math.max(0, Math.floor(diff / 86_400_000))
  }, [checkIn, checkOut])

  const stayTotal = isStay && nights > 0 ? basePrice * nights : basePrice

  const taxiEstimate = useMemo(() => {
    if (!isTaxi || !from.trim() || !to.trim()) return 0
    const mult = taxiType === 'VIP' ? 1.45 : taxiType === 'Van' ? 1.25 : 1
    return Math.round(Math.max(basePrice, 12) * mult)
  }, [isTaxi, from, to, taxiType, basePrice])

  const priceLine =
    service.price != null
      ? isStay && nights > 0
        ? `${service.currency} ${stayTotal.toLocaleString()} (${nights} night${nights !== 1 ? 's' : ''})`
        : `${service.currency} ${service.price}`
      : t('landingBooking.priceOnRequest')

  const today = new Date().toISOString().slice(0, 10)

  const canConfirmStay = isStay ? checkIn && checkOut && nights > 0 && (roomPhase ? selectedRoom != null : true) : true
  const canConfirmTaxi = taxiEstimate > 0

  function buildCheckoutUrl() {
    const params = new URLSearchParams({ serviceId: service.id })
    if (checkIn) params.set('checkIn', checkIn)
    if (checkOut) params.set('checkOut', checkOut)
    if (guests > 1) params.set('guests', String(guests))
    if (isTaxi) {
      params.set('from', from)
      params.set('to', to)
      params.set('taxiType', taxiType)
    }
    return `/checkout?${params.toString()}`
  }

  function proceed() {
    const checkoutUrl = buildCheckoutUrl()
    if (isAuthenticated) {
      navigate(checkoutUrl)
    } else {
      navigate(`/login?next=${encodeURIComponent(checkoutUrl)}`)
    }
  }

  return (
    <div className="mt-8">
      {/* Date / guest picker for hotels & resorts */}
      {isStay ? (
        <div className="mb-4 rounded-[20px] border p-5" style={{ borderColor: 'rgba(90, 122, 130, 0.2)', background: 'rgba(255, 255, 255, 0.92)' }}>
          <h2 className="text-lg font-semibold" style={{ color: 'var(--ink-heading)' }}>
            {t('landingBooking.selectDates')}
          </h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <label className="block">
              <span className="text-xs font-medium uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
                {t('landingBooking.checkIn')}
              </span>
              <input
                type="date"
                min={today}
                value={checkIn}
                onChange={(e) => {
                  setCheckIn(e.target.value)
                  if (checkOut && e.target.value >= checkOut) setCheckOut('')
                }}
                className="mt-1 w-full rounded-xl border px-3 py-2 text-sm outline-none focus:ring-2"
                style={{ borderColor: 'rgba(90,122,130,0.25)', color: 'var(--ink-heading)' }}
              />
            </label>
            <label className="block">
              <span className="text-xs font-medium uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
                {t('landingBooking.checkOut')}
              </span>
              <input
                type="date"
                min={checkIn || today}
                value={checkOut}
                onChange={(e) => setCheckOut(e.target.value)}
                className="mt-1 w-full rounded-xl border px-3 py-2 text-sm outline-none focus:ring-2"
                style={{ borderColor: 'rgba(90,122,130,0.25)', color: 'var(--ink-heading)' }}
              />
            </label>
          </div>
          <label className="mt-3 block">
            <span className="text-xs font-medium uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
              {t('landingBooking.guests')}
            </span>
            <div className="mt-1 flex items-center gap-3">
              <button
                type="button"
                onClick={() => setGuests((g) => Math.max(1, g - 1))}
                className="flex h-8 w-8 items-center justify-center rounded-full border text-base font-bold"
                style={{ borderColor: 'rgba(90,122,130,0.3)', color: 'var(--ink-heading)' }}
              >
                −
              </button>
              <span className="w-6 text-center font-semibold" style={{ color: 'var(--ink-heading)' }}>
                {guests}
              </span>
              <button
                type="button"
                onClick={() => setGuests((g) => Math.min(20, g + 1))}
                className="flex h-8 w-8 items-center justify-center rounded-full border text-base font-bold"
                style={{ borderColor: 'rgba(90,122,130,0.3)', color: 'var(--ink-heading)' }}
              >
                +
              </button>
            </div>
          </label>
        </div>
      ) : null}

      {/* Room selector (optional, second step for stays) */}
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
                  {n}
                </button>
              )
            })}
          </div>
        </div>
      ) : null}

      {/* Taxi route picker */}
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
            {TAXI_TYPES.map((type) => (
              <button
                key={type}
                type="button"
                onClick={() => setTaxiType(type)}
                className="rounded-xl border-2 px-4 py-2 text-sm font-semibold transition-colors"
                style={{
                  borderColor: taxiType === type ? 'var(--accent-teal)' : 'rgba(90, 122, 130, 0.25)',
                  background: taxiType === type ? 'rgba(90, 160, 150, 0.15)' : 'transparent',
                  color: 'var(--ink-heading)',
                }}
              >
                {type}
              </button>
            ))}
          </div>
        </div>
      ) : null}

      {/* Sticky bottom bar */}
      <div
        className="fixed inset-x-0 bottom-0 z-40 border-t px-4 py-4 shadow-[0_-8px_24px_rgba(15,23,42,0.08)] sm:px-8"
        style={{ background: 'rgba(255, 255, 255, 0.96)', borderColor: 'rgba(90, 122, 130, 0.15)' }}
      >
        <div className="mx-auto flex max-w-3xl flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <p className="text-xs font-medium uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
              {isStay
                ? nights > 0
                  ? t('landingBooking.totalForNights')
                  : t('landingBooking.pricePerNight')
                : isTaxi
                  ? t('landingBooking.estimateLabel')
                  : t('landingBooking.startingFrom')}
            </p>
            <p className="truncate text-xl font-bold" style={{ color: 'var(--ink-heading)' }}>
              {isTaxi ? (canConfirmTaxi ? `${service.currency} ${taxiEstimate}` : '—') : priceLine}
            </p>
          </div>
          <div className="flex shrink-0 flex-col gap-2 sm:flex-row">
            {isStay && !roomPhase ? (
              <button
                type="button"
                disabled={!canConfirmStay}
                onClick={() => (checkIn && checkOut && nights > 0 ? setRoomPhase(true) : undefined)}
                className="lp-btn lp-btn-primary px-6 py-3 text-center disabled:cursor-not-allowed disabled:opacity-50"
              >
                {t('landingBooking.selectRoom')}
              </button>
            ) : null}
            {isStay && roomPhase ? (
              <button
                type="button"
                disabled={!canConfirmStay}
                onClick={proceed}
                className="lp-btn lp-btn-primary px-6 py-3 text-center disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isAuthenticated ? t('landingBooking.confirmBooking') : t('landingBooking.confirmContinue')}
              </button>
            ) : null}
            {!isStay && !isTaxi ? (
              <button type="button" onClick={proceed} className="lp-btn lp-btn-primary px-6 py-3 text-center">
                {isAuthenticated ? t('landingBooking.confirmBooking') : t('landingBooking.bookNow')}
              </button>
            ) : null}
            {isTaxi ? (
              <button
                type="button"
                disabled={!canConfirmTaxi}
                onClick={proceed}
                className="lp-btn lp-btn-primary px-6 py-3 text-center disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isAuthenticated ? t('landingBooking.confirmBooking') : t('landingBooking.requestRide')}
              </button>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  )
}
