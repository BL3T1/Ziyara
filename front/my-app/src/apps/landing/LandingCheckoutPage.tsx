import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { bookingsAPI, getApiErrorMessage, servicesAPI } from '../../services/api'
import type { ServiceDto } from '../../types/api'
import { useLanguage } from '../../context/LanguageContext'
import { useAuth } from '../../context/AuthContext'
import { checkoutSchema } from '../../lib/validation'

export function LandingCheckoutPage() {
  const { t } = useLanguage()
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const serviceId = searchParams.get('serviceId') ?? ''
  const checkIn = searchParams.get('checkIn') ?? ''
  const checkOut = searchParams.get('checkOut') ?? ''
  const guests = parseInt(searchParams.get('guests') ?? '1', 10)
  const discountCode = searchParams.get('discount') ?? ''

  const [service, setService] = useState<ServiceDto | null>(null)
  const [loadingService, setLoadingService] = useState(true)
  const [coupon, setCoupon] = useState(discountCode)
  const [specialRequests, setSpecialRequests] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [bookingRef, setBookingRef] = useState<string | null>(null)

  // Redirect to login if not authenticated
  useEffect(() => {
    if (!isAuthenticated) {
      const next = `/checkout?${searchParams.toString()}`
      navigate(`/login?next=${encodeURIComponent(next)}`, { replace: true })
    }
  }, [isAuthenticated, navigate, searchParams])

  useEffect(() => {
    if (!serviceId) { setLoadingService(false); return }
    servicesAPI.get(serviceId)
      .then((res) => setService(res.data as ServiceDto))
      .catch(() => setService(null))
      .finally(() => setLoadingService(false))
  }, [serviceId])

  const nights = (() => {
    if (!checkIn || !checkOut) return 0
    const diff = new Date(checkOut).getTime() - new Date(checkIn).getTime()
    return Math.max(0, Math.floor(diff / 86_400_000))
  })()

  const pricePerNight = Number(service?.basePrice ?? 0)
  const subtotal = nights > 0 ? pricePerNight * nights : pricePerNight
  const currency = service?.currency ?? 'USD'

  async function confirmBooking() {
    if (!serviceId) return
    setError('')
    const parsed = checkoutSchema.safeParse({ discountCode: coupon, specialRequests })
    if (!parsed.success) {
      setError(parsed.error.issues[0].message)
      return
    }
    setSubmitting(true)
    try {
      const res = await bookingsAPI.create({
        serviceId,
        checkInDate: checkIn,
        checkOutDate: checkOut || undefined,
        guests,
        discountCode: coupon || undefined,
        specialRequests: specialRequests || undefined,
        currency,
      })
      const data = res.data as { referenceNumber?: string; id?: string } | null
      setBookingRef(data?.referenceNumber ?? data?.id ?? 'confirmed')
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Booking failed. Please try again.'))
    } finally {
      setSubmitting(false)
    }
  }

  if (!isAuthenticated) return null

  if (bookingRef) {
    return (
      <div className="lp-www-inner flex min-h-screen flex-col items-center justify-center py-12">
        <div className="lp-sheet w-full max-w-md text-center">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100">
            <svg className="h-8 w-8 text-emerald-600" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
          <h1 className="lp-h1 !text-2xl">{t('checkout.successTitle')}</h1>
          <p className="lp-body mt-2 text-sm">{t('checkout.successBody')}</p>
          <p className="mt-4 rounded-xl bg-slate-50 px-4 py-3 font-mono text-sm font-bold" style={{ color: 'var(--accent-teal)' }}>
            {bookingRef}
          </p>
          <button
            type="button"
            onClick={() => navigate('/services')}
            className="lp-btn lp-btn-primary mt-6 w-full py-3 text-center"
          >
            {t('checkout.browseMore')}
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="lp-www-inner min-h-screen py-10">
      <div className="mx-auto w-full max-w-lg">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="lp-link-quiet mb-6 inline-flex items-center gap-1 text-sm font-medium"
          style={{ color: 'var(--accent-teal)' }}
        >
          ← {t('checkout.back')}
        </button>

        <h1 className="lp-h1 !text-2xl">{t('checkout.title')}</h1>

        {loadingService ? (
          <p className="lp-muted mt-4">{t('ui.loading')}</p>
        ) : !service ? (
          <p className="mt-4 text-sm text-red-600">{t('checkout.serviceNotFound')}</p>
        ) : (
          <div className="mt-6 space-y-4">
            {/* Service summary card */}
            <div className="lp-sheet !p-5">
              <h2 className="text-base font-semibold" style={{ color: 'var(--ink-heading)' }}>
                {service.name}
              </h2>
              {service.city || service.country ? (
                <p className="mt-0.5 text-sm" style={{ color: 'var(--ink-muted)' }}>
                  {[service.city, service.country].filter(Boolean).join(', ')}
                </p>
              ) : null}

              {checkIn || checkOut ? (
                <div className="mt-3 grid grid-cols-2 gap-3 text-sm">
                  {checkIn ? (
                    <div>
                      <span className="block text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
                        {t('landingBooking.checkIn')}
                      </span>
                      <span style={{ color: 'var(--ink-heading)' }}>{checkIn}</span>
                    </div>
                  ) : null}
                  {checkOut ? (
                    <div>
                      <span className="block text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
                        {t('landingBooking.checkOut')}
                      </span>
                      <span style={{ color: 'var(--ink-heading)' }}>{checkOut}</span>
                    </div>
                  ) : null}
                </div>
              ) : null}

              {nights > 0 ? (
                <p className="mt-2 text-sm" style={{ color: 'var(--ink-muted)' }}>
                  {nights} night{nights !== 1 ? 's' : ''} · {guests} guest{guests !== 1 ? 's' : ''}
                </p>
              ) : null}

              <div className="mt-4 border-t pt-4" style={{ borderColor: 'rgba(90,122,130,0.15)' }}>
                <div className="flex justify-between text-sm">
                  <span style={{ color: 'var(--ink-muted)' }}>
                    {nights > 0
                      ? `${currency} ${pricePerNight} × ${nights} night${nights !== 1 ? 's' : ''}`
                      : t('checkout.basePrice')}
                  </span>
                  <span className="font-semibold" style={{ color: 'var(--ink-heading)' }}>
                    {currency} {subtotal.toLocaleString()}
                  </span>
                </div>
              </div>
            </div>

            {/* Discount code */}
            <div className="lp-sheet !p-5">
              <label className="block text-sm font-semibold" style={{ color: 'var(--ink-heading)' }}>
                {t('checkout.discountCode')}
              </label>
              <input
                type="text"
                value={coupon}
                onChange={(e) => setCoupon(e.target.value.toUpperCase())}
                placeholder="e.g. SUMMER20"
                className="mt-2 w-full rounded-xl border px-3 py-2.5 text-sm font-mono uppercase outline-none focus:ring-2"
                style={{ borderColor: 'rgba(90,122,130,0.25)', color: 'var(--ink-heading)' }}
              />
            </div>

            {/* Special requests */}
            <div className="lp-sheet !p-5">
              <label className="block text-sm font-semibold" style={{ color: 'var(--ink-heading)' }}>
                {t('checkout.specialRequests')}
              </label>
              <textarea
                value={specialRequests}
                onChange={(e) => setSpecialRequests(e.target.value)}
                rows={3}
                placeholder={t('checkout.specialRequestsPlaceholder')}
                className="mt-2 w-full rounded-xl border px-3 py-2.5 text-sm outline-none focus:ring-2"
                style={{ borderColor: 'rgba(90,122,130,0.25)', color: 'var(--ink-heading)' }}
              />
            </div>

            {error ? (
              <p className="rounded-xl border border-red-200/80 bg-red-50/90 px-3 py-2.5 text-sm text-red-900">
                {error}
              </p>
            ) : null}

            <button
              type="button"
              disabled={submitting}
              onClick={confirmBooking}
              className="lp-btn lp-btn-primary w-full py-4 text-center text-base font-bold disabled:opacity-60"
            >
              {submitting ? t('checkout.confirming') : `${t('checkout.confirmPay')} ${currency} ${subtotal.toLocaleString()}`}
            </button>

            <p className="text-center text-xs" style={{ color: 'var(--ink-muted)' }}>
              {t('checkout.termsNote')}
            </p>
          </div>
        )}
      </div>
    </div>
  )
}
