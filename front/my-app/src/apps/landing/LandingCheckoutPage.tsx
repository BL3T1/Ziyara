import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { bookingsAPI, discountsAPI, getApiErrorMessage, servicesAPI } from '../../services/api'
import type { ServiceDto, VoucherDto } from '../../types/api'
import { useLanguage } from '../../context/LanguageContext'
import { useAuth } from '../../context/AuthContext'
import { checkoutSchema } from '../../lib/validation'
import { downloadVoucherPdf } from './voucherPdf'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'

const MAX_SPECIAL_REQUESTS = 500

type PaymentMethodId = 'CREDIT_CARD' | 'CASH_ON_ARRIVAL' | 'CRYPTO_WALLET' | 'PAYPAL' | 'GOOGLE_PAY'

const PAYMENT_METHODS: { id: PaymentMethodId; icon: string; labelKey: string; descKey: string }[] = [
  { id: 'CREDIT_CARD',    icon: '💳', labelKey: 'checkout.pm.creditCard',  descKey: 'checkout.pm.creditCardDesc'  },
  { id: 'PAYPAL',         icon: '🅿️', labelKey: 'checkout.pm.paypal',      descKey: 'checkout.pm.paypalDesc'      },
  { id: 'GOOGLE_PAY',     icon: '📱', labelKey: 'checkout.pm.googlePay',    descKey: 'checkout.pm.googlePayDesc'   },
  { id: 'CRYPTO_WALLET',  icon: '₿',  labelKey: 'checkout.pm.crypto',       descKey: 'checkout.pm.cryptoDesc'      },
  { id: 'CASH_ON_ARRIVAL',icon: '💵', labelKey: 'checkout.pm.cash',         descKey: 'checkout.pm.cashDesc'        },
]

function CheckoutSkeleton() {
  return (
    <div className="mt-6 space-y-4 animate-pulse">
      <div className="lp-sheet !p-5 space-y-3">
        <div className="h-5 w-1/2 rounded-md lp-skeleton" />
        <div className="h-4 w-1/3 rounded-md lp-skeleton-light" />
        <div className="mt-3 h-16 w-full rounded-xl lp-skeleton-light" />
      </div>
      <div className="lp-sheet !p-5 space-y-2">
        <div className="h-4 w-1/4 rounded-md lp-skeleton" />
        <div className="h-10 w-full rounded-xl lp-skeleton-light" />
      </div>
      <div className="h-14 w-full rounded-2xl" style={{ background: 'rgba(160,123,86,0.25)' }} />
    </div>
  )
}

type CouponState = 'idle' | 'checking' | 'valid' | 'invalid'

export function LandingCheckoutPage() {
  const { t } = useLanguage()
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const serviceId = searchParams.get('serviceId') ?? ''
  const checkIn = searchParams.get('checkIn') ?? ''
  const checkOut = searchParams.get('checkOut') ?? ''
  const guests = parseInt(searchParams.get('guests') ?? '1', 10)
  const initialDiscount = searchParams.get('discount') ?? ''

  const [service, setService] = useState<ServiceDto | null>(null)
  const [loadingService, setLoadingService] = useState(true)
  const [coupon, setCoupon] = useState(initialDiscount)
  const [couponState, setCouponState] = useState<CouponState>('idle')
  const [couponDiscount, setCouponDiscount] = useState(0)
  const [specialRequests, setSpecialRequests] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [bookingRef, setBookingRef] = useState<string | null>(null)
  const [bookingId, setBookingId] = useState<string | null>(null)
  const [voucherLoading, setVoucherLoading] = useState(false)
  const [copied, setCopied] = useState(false)
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethodId>('CREDIT_CARD')
  useDocumentMeta({ title: bookingRef ? 'Booking Confirmed · Ziyara' : 'Checkout · Ziyara' })

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
  const subtotal = Math.max(0, (nights > 0 ? pricePerNight * nights : pricePerNight) - couponDiscount)
  const currency = service?.currency ?? 'USD'

  async function applyCode() {
    if (!coupon.trim()) return
    setCouponState('checking')
    try {
      const amount = nights > 0 ? pricePerNight * nights : pricePerNight
      const res = await discountsAPI.validate(coupon, amount)
      const data = res.data as { valid?: boolean; discountAmount?: number } | null
      if (data?.valid) {
        setCouponState('valid')
        setCouponDiscount(Number(data.discountAmount ?? 0))
      } else {
        setCouponState('invalid')
        setCouponDiscount(0)
      }
    } catch {
      setCouponState('invalid')
      setCouponDiscount(0)
    }
  }

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
        paymentMethod,
      })
      const data = res.data as { referenceNumber?: string; id?: string; bookingReference?: string } | null
      setBookingRef(data?.referenceNumber ?? data?.bookingReference ?? 'confirmed')
      setBookingId(data?.id ?? null)
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Booking failed. Please try again.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCopyRef() {
    if (!bookingRef) return
    try {
      await navigator.clipboard.writeText(bookingRef)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch { /* ignore */ }
  }

  if (!isAuthenticated) return null

  if (bookingRef) {
    const handleVoucherDownload = async () => {
      if (!bookingId) return
      setVoucherLoading(true)
      try {
        const res = await bookingsAPI.getVoucher(bookingId)
        downloadVoucherPdf(res.data as VoucherDto)
      } catch { /* ignore */ }
      finally { setVoucherLoading(false) }
    }

    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center py-12">
        <div className="lp-sheet w-full max-w-md text-center">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full lp-skeleton-green">
            <svg className="h-8 w-8" fill="none" stroke="#22a06b" strokeWidth="2.2" viewBox="0 0 24 24" aria-hidden>
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
          <h1 className="lp-h1 !text-2xl">{t('checkout.successTitle')}</h1>
          <p className="lp-muted mt-2 text-sm">{t('checkout.successBody')}</p>

          <div className="mt-5 rounded-2xl p-4 lp-confirm-panel">
            <p className="mb-1 text-xs font-semibold uppercase tracking-widest lp-text-accent">
              {t('checkout.bookingRefLabel')}
            </p>
            <div className="flex items-center justify-center gap-3">
              <p className="font-mono text-xl font-bold lp-text-heading">{bookingRef}</p>
              <button
                type="button"
                onClick={handleCopyRef}
                className="rounded-lg border px-2.5 py-1 text-xs font-medium transition-all"
                style={{ borderColor: 'rgba(61,112,128,0.3)', color: 'var(--accent-teal)', background: copied ? 'rgba(61,112,128,0.1)' : 'transparent' }}
              >
                {copied ? t('checkout.copiedRef') : t('checkout.copyRef')}
              </button>
            </div>
          </div>

          {service && (
            <div className="mt-4 rounded-xl px-4 py-3 text-left text-sm lp-bg-white-glass-border">
              <p className="font-semibold lp-text-heading">{service.name}</p>
              {checkIn && <p className="mt-0.5 lp-text-muted">{t('landingBooking.checkIn')}: {checkIn}{checkOut ? ` → ${checkOut}` : ''}</p>}
              {nights > 0 && <p className="lp-text-muted">{nights} {nights !== 1 ? t('checkout.nights') : t('checkout.night')}</p>}
            </div>
          )}

          <div className="mt-6 flex flex-col gap-3">
            {bookingId && (
              <button
                type="button"
                disabled={voucherLoading}
                onClick={handleVoucherDownload}
                className="lp-btn lp-btn-primary w-full py-3 text-center disabled:opacity-60"
              >
                {voucherLoading ? '…' : t('checkout.downloadVoucher')}
              </button>
            )}
            <Link to="/my-bookings" className="lp-btn lp-btn-outline w-full py-3 text-center">
              {t('checkout.viewBookings')}
            </Link>
            <button type="button" onClick={() => navigate('/services')} className="lp-link-quiet py-2 text-sm font-medium">
              {t('checkout.browseMore')}
            </button>
          </div>
        </div>
      </div>
    )
  }

  const TYPE_TO_SLUG: Record<string, string> = {
    HOTEL: 'hotels', RESORT: 'resorts', RESTAURANT: 'restaurants', TAXI: 'taxis', TRIP: 'trips',
  }
  const backPath = serviceId && service
    ? `/${TYPE_TO_SLUG[service.type ?? ''] ?? 'hotels'}/${serviceId}`
    : undefined

  return (
    <div className="min-h-[60vh] py-10">
      <div className="mx-auto w-full max-w-lg">
        <button
          type="button"
          onClick={() => backPath ? navigate(backPath) : navigate(-1)}
          className="lp-link-quiet mb-6 inline-flex items-center gap-1 text-sm font-medium lp-text-accent"
        >
          ← {t('checkout.back')}{service ? ` ${service.name}` : ''}
        </button>

        <h1 className="lp-h1 !text-2xl">{t('checkout.title')}</h1>

        {loadingService ? (
          <CheckoutSkeleton />
        ) : !service ? (
          <p className="mt-4 text-sm text-red-600">{t('checkout.serviceNotFound')}</p>
        ) : (
          <div className="mt-6 space-y-4">
            {/* Service summary */}
            <div className="lp-sheet !p-5">
              <h2 className="text-base font-semibold lp-text-heading">{service.name}</h2>
              {service.city || service.country ? (
                <p className="mt-0.5 text-sm lp-text-muted">
                  {[service.city, service.country].filter(Boolean).join(', ')}
                </p>
              ) : null}
              {checkIn || checkOut ? (
                <div className="mt-3 grid grid-cols-2 gap-3 text-sm">
                  {checkIn && (
                    <div>
                      <span className="block text-xs font-semibold uppercase tracking-wide lp-text-muted">{t('landingBooking.checkIn')}</span>
                      <span className="lp-text-heading">{checkIn}</span>
                    </div>
                  )}
                  {checkOut && (
                    <div>
                      <span className="block text-xs font-semibold uppercase tracking-wide lp-text-muted">{t('landingBooking.checkOut')}</span>
                      <span className="lp-text-heading">{checkOut}</span>
                    </div>
                  )}
                </div>
              ) : null}
              {nights > 0 ? (
                <p className="mt-2 text-sm lp-text-muted">
                  {nights} {nights !== 1 ? t('checkout.nights') : t('checkout.night')} · {guests} {t('landingBooking.guests').toLowerCase()}
                </p>
              ) : null}
              <div className="mt-4 border-t pt-4 lp-border-faint">
                <div className="flex justify-between text-sm">
                  <span className="lp-text-muted">
                    {nights > 0 ? `${currency} ${pricePerNight} × ${nights} ${nights !== 1 ? t('checkout.nights') : t('checkout.night')}` : t('checkout.basePrice')}
                  </span>
                  <span className="font-semibold lp-text-heading">
                    {currency} {(nights > 0 ? pricePerNight * nights : pricePerNight).toLocaleString()}
                  </span>
                </div>
                {couponState === 'valid' && couponDiscount > 0 && (
                  <div className="mt-2 flex justify-between border-t pt-2 text-sm lp-border-soft">
                    <span className="lp-text-accent">Discount ({coupon})</span>
                    <span className="font-semibold lp-text-accent">− {currency} {couponDiscount.toLocaleString()}</span>
                  </div>
                )}
                {couponState === 'valid' && couponDiscount > 0 && (
                  <div className="mt-2 flex justify-between border-t pt-2 text-sm font-bold lp-border-faint">
                    <span className="lp-text-heading">Total</span>
                    <span className="lp-text-heading">{currency} {subtotal.toLocaleString()}</span>
                  </div>
                )}
              </div>
            </div>

            {/* Discount code with inline validation */}
            <div className="lp-sheet !p-5">
              <label className="block text-sm font-semibold lp-text-heading">
                {t('checkout.discountCode')}
              </label>
              <div className="mt-2 flex gap-2">
                <input
                  type="text"
                  value={coupon}
                  onChange={(e) => { setCoupon(e.target.value.toUpperCase()); setCouponState('idle'); setCouponDiscount(0) }}
                  placeholder={t('checkout.discountCodePlaceholder')}
                  className="flex-1 rounded-xl border px-3 py-2.5 text-sm font-mono uppercase outline-none focus:ring-2"
                  style={{
                    borderColor: couponState === 'valid' ? 'var(--accent-teal)' : couponState === 'invalid' ? '#e74c3c' : 'rgba(90,122,130,0.25)',
                    color: 'var(--ink-heading)',
                  }}
                />
                <button
                  type="button"
                  onClick={applyCode}
                  disabled={!coupon.trim() || couponState === 'checking' || couponState === 'valid'}
                  className="lp-btn lp-btn-outline lp-btn-sm shrink-0 disabled:opacity-50"
                >
                  {couponState === 'checking' ? '…' : t('checkout.applyCode')}
                </button>
              </div>
              {couponState === 'valid' && (
                <p className="mt-1.5 flex items-center gap-1 text-xs font-medium lp-text-accent">
                  <span>✓</span>
                  <span>{t('checkout.codeApplied', { amount: couponDiscount })}</span>
                </p>
              )}
              {couponState === 'invalid' && (
                <p className="mt-1.5 text-xs font-medium lp-text-error">
                  {t('checkout.codeInvalid')}
                </p>
              )}
            </div>

            {/* Special requests with character counter */}
            <div className="lp-sheet !p-5">
              <div className="flex items-baseline justify-between">
                <label className="block text-sm font-semibold lp-text-heading">
                  {t('checkout.specialRequests')}
                </label>
                <span className="text-xs" style={{ color: specialRequests.length > MAX_SPECIAL_REQUESTS * 0.85 ? '#c0392b' : 'var(--ink-faint)' }}>
                  {specialRequests.length}/{MAX_SPECIAL_REQUESTS}
                </span>
              </div>
              <textarea
                value={specialRequests}
                onChange={(e) => setSpecialRequests(e.target.value)}
                rows={3}
                maxLength={MAX_SPECIAL_REQUESTS}
                placeholder={t('checkout.specialRequestsPlaceholder')}
                className="mt-2 w-full rounded-xl border px-3 py-2.5 text-sm outline-none focus:ring-2"
                style={{ borderColor: 'rgba(90,122,130,0.25)', color: 'var(--ink-heading)' }}
              />
            </div>

            {/* Payment method selection */}
            <div className="lp-sheet !p-5">
              <p className="text-sm font-semibold lp-text-heading">{t('checkout.paymentMethodTitle')}</p>
              <p className="mt-0.5 text-xs lp-text-muted">{t('checkout.paymentMethodSub')}</p>
              <div className="mt-3 grid grid-cols-1 gap-2">
                {PAYMENT_METHODS.map((pm) => {
                  const selected = paymentMethod === pm.id
                  return (
                    <button
                      key={pm.id}
                      type="button"
                      onClick={() => setPaymentMethod(pm.id)}
                      className="flex items-center gap-3 rounded-2xl border px-4 py-3 text-left transition-all focus:outline-none focus:ring-2"
                      style={{
                        borderColor: selected ? 'var(--accent-teal)' : 'rgba(90,122,130,0.2)',
                        background: selected ? 'rgba(61,112,128,0.07)' : 'transparent',
                        boxShadow: selected ? '0 0 0 1px var(--accent-teal)' : 'none',
                      }}
                      aria-pressed={selected}
                    >
                      <span className="text-xl leading-none select-none" aria-hidden="true">{pm.icon}</span>
                      <span className="flex-1 min-w-0">
                        <span className="block text-sm font-semibold lp-text-heading">{t(pm.labelKey)}</span>
                        <span className="block text-xs lp-text-muted">{t(pm.descKey)}</span>
                      </span>
                      {selected && (
                        <svg className="h-4 w-4 shrink-0" fill="none" stroke="var(--accent-teal)" strokeWidth="2.5" viewBox="0 0 24 24" aria-hidden>
                          <polyline points="20 6 9 17 4 12" />
                        </svg>
                      )}
                    </button>
                  )
                })}
              </div>
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
              {submitting ? t('checkout.confirming') : `${t('checkout.confirmPay')} · ${currency} ${subtotal.toLocaleString()}`}
            </button>

            <p className="text-center text-xs lp-text-faint">
              {t('checkout.paymentNote')}
            </p>
            <p className="text-center text-xs lp-text-muted">
              {t('checkout.termsNote')}
            </p>
          </div>
        )}
      </div>
    </div>
  )
}
