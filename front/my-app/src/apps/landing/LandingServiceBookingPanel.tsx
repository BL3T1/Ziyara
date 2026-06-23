import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { ServiceEntity } from '../../pages/services/serviceModel'
import type { HotelRoomDto } from '../../types/api'
import { useLanguage } from '../../context/LanguageContext'
import { useAuth } from '../../context/AuthContext'
import { servicesAPI } from '../../services/api'

type AvailState = 'idle' | 'checking' | 'available' | 'unavailable'

const TAXI_TYPES = ['Economy', 'VIP', 'Van'] as const

interface LandingServiceBookingPanelProps {
  service: ServiceEntity
}

function BookingCard({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return (
    <div
      className={`mb-4 rounded-[22px] border p-5 sm:p-6 ${className}`}
      style={{
        borderColor: 'rgba(255, 255, 255, 0.72)',
        background: 'linear-gradient(145deg, rgba(255,255,255,0.72) 0%, rgba(255,255,255,0.44) 100%)',
        boxShadow: '0 8px 32px rgba(40,55,68,0.07), inset 0 1px 0 rgba(255,255,255,0.9)',
        backdropFilter: 'blur(14px)',
      }}
    >
      {children}
    </div>
  )
}

export function LandingServiceBookingPanel({ service }: LandingServiceBookingPanelProps) {
  const { t } = useLanguage()
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()

  const [checkIn, setCheckIn] = useState('')
  const [checkOut, setCheckOut] = useState('')
  const [guests, setGuests] = useState(1)
  const [roomPhase, setRoomPhase] = useState(false)
  const [selectedRoom, setSelectedRoom] = useState<string | null>(null)
  const [rooms, setRooms] = useState<HotelRoomDto[]>([])
  const [roomsLoading, setRoomsLoading] = useState(false)
  const [avail, setAvail] = useState<AvailState>('idle')
  const [availMsg, setAvailMsg] = useState('')
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)
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

  // Availability check: debounce 300 ms after dates change for stays
  useEffect(() => {
    if (!isStay || !checkIn || nights <= 0) { Promise.resolve().then(() => setAvail('idle')); return }
    Promise.resolve().then(() => setAvail('checking'))
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(async () => {
      try {
        const res = await servicesAPI.checkAvailability(service.id, checkIn, nights)
        const data = res.data as { available?: boolean; message?: string } | null
        setAvail(data?.available ? 'available' : 'unavailable')
        setAvailMsg(data?.message ?? '')
      } catch {
        setAvail('idle')
      }
    }, 300)
    return () => { if (debounceRef.current) clearTimeout(debounceRef.current) }
  }, [isStay, service.id, checkIn, nights])

  // Fetch real room types when the hotel room picker is opened
  useEffect(() => {
    if (!isStay || !roomPhase || rooms.length > 0) return
    setRoomsLoading(true)
    servicesAPI.listRooms(service.id)
      .then((res) => setRooms(res.data as HotelRoomDto[]))
      .catch(() => setRooms([]))
      .finally(() => setRoomsLoading(false))
  }, [isStay, roomPhase, service.id, rooms.length])

  const stayTotal = isStay && nights > 0 ? basePrice * nights : basePrice

  const priceLine =
    service.price != null
      ? isStay && nights > 0
        ? `${service.currency} ${stayTotal.toLocaleString()} (${nights} night${nights !== 1 ? 's' : ''})`
        : `${service.currency} ${service.price}`
      : t('landingBooking.priceOnRequest')

  const today = new Date().toISOString().slice(0, 10)

  const availOk = !isStay || avail === 'idle' || avail === 'available'
  const canConfirmStay = isStay ? checkIn && checkOut && nights > 0 && availOk && (roomPhase ? selectedRoom != null : true) : true
  const canConfirmTaxi = isTaxi && from.trim().length > 0 && to.trim().length > 0

  function buildCheckoutUrl() {
    const params = new URLSearchParams({ serviceId: service.id })
    if (checkIn) params.set('checkIn', checkIn)
    if (checkOut) params.set('checkOut', checkOut)
    if (guests > 1) params.set('guests', String(guests))
    if (selectedRoom) params.set('roomTypeId', selectedRoom)
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

  const inputCls = 'lp-input mt-1'

  return (
    <div className="mt-8">
      {/* Date / guest picker for hotels & resorts */}
      {isStay ? (
        <BookingCard>
          <p className="lp-eyebrow lp-eyebrow--tight">{t('landingBooking.selectDates')}</p>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <label className="block">
              <span className="lp-label">{t('landingBooking.checkIn')}</span>
              <input
                type="date"
                min={today}
                value={checkIn}
                onChange={(e) => {
                  setCheckIn(e.target.value)
                  if (checkOut && e.target.value >= checkOut) setCheckOut('')
                }}
                className={inputCls}
              />
            </label>
            <label className="block">
              <span className="lp-label">{t('landingBooking.checkOut')}</span>
              <input
                type="date"
                min={checkIn || today}
                value={checkOut}
                onChange={(e) => setCheckOut(e.target.value)}
                className={inputCls}
              />
            </label>
          </div>
          {avail === 'checking' && (
            <div className="lp-avail-banner" data-status="checking">
              <span>{t('landingBooking.checkingAvailability') || 'Checking availability…'}</span>
            </div>
          )}
          {avail === 'available' && (
            <div className="lp-avail-banner" data-status="available">
              <span>✓ {t('landingBooking.available') || 'Available for your dates'}</span>
            </div>
          )}
          {avail === 'unavailable' && (
            <div className="lp-avail-banner" data-status="unavailable">
              <span>✕ {availMsg || t('landingBooking.unavailable') || 'Not available for these dates'}</span>
            </div>
          )}
          <div className="mt-4">
            <span className="lp-label">{t('landingBooking.guests')}</span>
            <div className="mt-2 flex items-center gap-4">
              <button
                type="button"
                onClick={() => setGuests((g) => Math.max(1, g - 1))}
                className="flex h-9 w-9 items-center justify-center rounded-full border-2 text-lg font-bold transition-colors"
                style={{ borderColor: 'rgba(61,112,128,0.35)', color: 'var(--accent-teal)', background: 'rgba(255,255,255,0.7)' }}
              >
                −
              </button>
              <span className="w-8 text-center text-lg font-bold lp-text-heading">
                {guests}
              </span>
              <button
                type="button"
                onClick={() => setGuests((g) => Math.min(20, g + 1))}
                className="flex h-9 w-9 items-center justify-center rounded-full border-2 text-lg font-bold transition-colors"
                style={{ borderColor: 'rgba(61,112,128,0.35)', color: 'var(--accent-teal)', background: 'rgba(255,255,255,0.7)' }}
              >
                +
              </button>
            </div>
          </div>
        </BookingCard>
      ) : null}

      {/* Room selector */}
      {isStay && roomPhase ? (
        <BookingCard>
          <p className="lp-eyebrow lp-eyebrow--tight">{t('landingBooking.chooseRoomTitle')}</p>
          {roomsLoading ? (
            <p className="mt-3 text-sm lp-muted">Loading room types…</p>
          ) : rooms.length === 0 ? (
            <p className="mt-3 text-sm lp-muted">{t('landingBooking.roomLegend')}</p>
          ) : (
            <div className="mt-4 space-y-2">
              {rooms
                .filter((r) => r.status === 'ACTIVE' && r.quantityAvailable > 0)
                .map((room) => {
                  const selected = selectedRoom === room.id
                  return (
                    <button
                      key={room.id}
                      type="button"
                      onClick={() => setSelectedRoom(selected ? null : room.id)}
                      className="w-full rounded-xl border-2 px-4 py-3 text-left transition-all"
                      style={{
                        borderColor: selected ? 'var(--accent-teal)' : 'rgba(90, 122, 130, 0.25)',
                        background: selected ? 'rgba(61, 112, 128, 0.10)' : 'rgba(255,255,255,0.6)',
                      }}
                    >
                      <div className="flex items-center justify-between gap-3">
                        <div className="min-w-0">
                          <p className="font-semibold text-sm" style={{ color: selected ? 'var(--accent-teal)' : 'var(--ink-heading)' }}>
                            {room.roomName}
                          </p>
                          <p className="text-xs mt-0.5" style={{ color: 'var(--ink-muted)' }}>
                            {room.roomType}
                            {room.capacity ? ` · ${room.capacity} guests` : ''}
                          </p>
                          {room.description ? (
                            <p className="text-xs mt-0.5 truncate" style={{ color: 'var(--ink-faint)' }}>{room.description}</p>
                          ) : null}
                        </div>
                        {room.basePrice != null ? (
                          <p className="shrink-0 font-bold text-base" style={{ color: 'var(--accent-tan-mid)' }}>
                            {room.currency ?? service.currency ?? 'USD'} {room.basePrice.toLocaleString()}
                          </p>
                        ) : null}
                      </div>
                    </button>
                  )
                })}
            </div>
          )}
        </BookingCard>
      ) : null}

      {/* Taxi route picker */}
      {isTaxi ? (
        <BookingCard>
          <p className="lp-eyebrow lp-eyebrow--tight">{t('landingBooking.ridePlanTitle')}</p>
          <div className="mt-4 space-y-3">
            <label className="block">
              <span className="lp-label">{t('landingBooking.fromLabel')}</span>
              <input
                value={from}
                onChange={(e) => setFrom(e.target.value)}
                className={inputCls}
                placeholder={t('landingBooking.fromPlaceholder')}
              />
            </label>
            <label className="block">
              <span className="lp-label">{t('landingBooking.toLabel')}</span>
              <input
                value={to}
                onChange={(e) => setTo(e.target.value)}
                className={inputCls}
                placeholder={t('landingBooking.toPlaceholder')}
              />
            </label>
          </div>
          <div className="mt-4">
            <span className="lp-label">{t('landingBooking.vehicleType')}</span>
            <div className="mt-2 flex flex-wrap gap-2">
              {TAXI_TYPES.map((type) => (
                <button
                  key={type}
                  type="button"
                  onClick={() => setTaxiType(type)}
                  className="rounded-xl border-2 px-5 py-2 text-sm font-semibold transition-all"
                  style={{
                    borderColor: taxiType === type ? 'var(--accent-teal)' : 'rgba(90, 122, 130, 0.25)',
                    background: taxiType === type ? 'rgba(61, 112, 128, 0.1)' : 'rgba(255,255,255,0.6)',
                    color: taxiType === type ? 'var(--accent-teal)' : 'var(--ink-heading)',
                  }}
                >
                  {type}
                </button>
              ))}
            </div>
          </div>
        </BookingCard>
      ) : null}

      {/* Booking summary + CTA */}
      <div
        className="rounded-[22px] border p-5 sm:p-6"
        style={{
          borderColor: 'rgba(184, 150, 110, 0.3)',
          background: 'linear-gradient(135deg, rgba(184,150,110,0.08) 0%, rgba(61,112,128,0.06) 100%)',
          boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.8)',
        }}
      >
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div className="min-w-0">
            <p className="lp-eyebrow lp-eyebrow--tight" style={{ marginBottom: 4 }}>
              {isStay
                ? nights > 0
                  ? t('landingBooking.totalForNights')
                  : t('landingBooking.pricePerNight')
                : isTaxi
                  ? t('landingBooking.startingFrom')
                  : t('landingBooking.startingFrom')}
            </p>
            <p className="text-3xl font-bold" style={{ color: 'var(--ink-heading)', letterSpacing: '-0.02em' }}>
              {priceLine}
            </p>
          </div>

          <div className="flex shrink-0 flex-col gap-2 sm:flex-row">
            {isStay && !roomPhase ? (
              <button
                type="button"
                disabled={!checkIn || !checkOut || nights <= 0}
                onClick={() => setRoomPhase(true)}
                className="lp-btn lp-btn-primary px-7 py-3 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {t('landingBooking.selectRoom')}
              </button>
            ) : null}
            {isStay && roomPhase ? (
              <button
                type="button"
                disabled={!canConfirmStay}
                onClick={proceed}
                className="lp-btn lp-btn-primary px-7 py-3 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isAuthenticated ? t('landingBooking.confirmBooking') : t('landingBooking.confirmContinue')}
              </button>
            ) : null}
            {!isStay && !isTaxi ? (
              <button type="button" onClick={proceed} className="lp-btn lp-btn-primary px-7 py-3">
                {isAuthenticated ? t('landingBooking.confirmBooking') : t('landingBooking.bookNow')}
              </button>
            ) : null}
            {isTaxi ? (
              <button
                type="button"
                disabled={!canConfirmTaxi}
                onClick={proceed}
                className="lp-btn lp-btn-primary px-7 py-3 disabled:cursor-not-allowed disabled:opacity-50"
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
