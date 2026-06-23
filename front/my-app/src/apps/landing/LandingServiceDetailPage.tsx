import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { Card } from '../../components/Card'
import { ProviderMap } from '../../components/maps/ProviderMap'
import { getApiErrorMessage, servicesAPI } from '../../services/api'
import type { HotelRoomDto, RestaurantMenuDto, ReviewDto, ServiceDto, ServiceImageDto } from '../../types/api'
import { ServiceDetailView } from '../../pages/services/components/ServiceDetailView'
import { LandingServiceBookingPanel } from './LandingServiceBookingPanel'
import type { ServiceCategorySlug } from '../../pages/services/serviceModel'
import { normalizeService } from '../../pages/services/serviceModel'
import { sanitizeText, safeImageUrl } from '../../utils/safeRendering'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { useLanguage } from '../../context/LanguageContext'

function DetailSkeleton() {
  return (
    <div className="animate-pulse space-y-5">
      <div className="h-5 w-32 rounded-lg bg-slate-200/80" />
      <div className="h-64 w-full rounded-2xl bg-slate-200/80" />
      <div className="h-6 w-48 rounded-lg bg-slate-200/80" />
      <div className="space-y-2">
        <div className="h-4 w-full rounded-md bg-slate-200/80" />
        <div className="h-4 w-3/4 rounded-md bg-slate-200/80" />
      </div>
    </div>
  )
}

export function LandingServiceDetailPage() {
  const { category, id } = useParams<{ category: string; id: string }>()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { t } = useLanguage()
  const fromPath = pathname.split('/').filter(Boolean)
  const inferredCategory = fromPath[0] === 'services' ? fromPath[1] : fromPath[0]
  const seg = category ?? inferredCategory
  const safeCategory = (
    seg === 'hotels' || seg === 'resorts' || seg === 'restaurants' || seg === 'trips' || seg === 'taxis' ? seg : null
  ) as ServiceCategorySlug | null

  const [rawService, setRawService] = useState<ServiceDto | null>(null)
  const serviceName = rawService ? sanitizeText(rawService.name ?? '') : ''
  const serviceCity = rawService ? sanitizeText(rawService.city ?? '') : ''
  useDocumentMeta({
    title: serviceName ? `${serviceName}${serviceCity ? ` · ${serviceCity}` : ''} · Ziyara` : 'Ziyara',
    description: serviceName ? `Book ${serviceName} on Ziyara — instant confirmation, best rates.` : undefined,
  })
  const [menu, setMenu] = useState<RestaurantMenuDto | null>(null)
  const [rooms, setRooms] = useState<HotelRoomDto[]>([])
  const [reviews, setReviews] = useState<ReviewDto[]>([])
  const [images, setImages] = useState<ServiceImageDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    Promise.resolve().then(() => {
      setLoading(true)
      setError(null)
      return Promise.all([
        servicesAPI.get(id),
        servicesAPI.getImages(id).catch(() => ({ data: [] as ServiceImageDto[] })),
        servicesAPI.getMenu(id).catch(() => ({ data: { serviceId: id, sections: [] } as RestaurantMenuDto })),
        servicesAPI.listRooms(id).catch(() => ({ data: [] as HotelRoomDto[] })),
        servicesAPI.getServiceReviews(id).catch(() => ({ data: [] as ReviewDto[] })),
      ])
    })
      .then(([svcRes, imgRes, menuRes, roomsRes, reviewsRes]) => {
        const svcData = svcRes.data
        setRawService(svcData && typeof svcData === 'object' ? (svcData as ServiceDto) : null)
        setImages(Array.isArray(imgRes.data) ? imgRes.data : [])
        setMenu(menuRes.data && typeof menuRes.data === 'object' ? menuRes.data : { serviceId: id, sections: [] })
        setRooms(Array.isArray(roomsRes.data) ? roomsRes.data : [])
        setReviews(Array.isArray(reviewsRes.data) ? reviewsRes.data : [])
      })
      .catch((err) => {
        setRawService(null)
        setImages([])
        setMenu(null)
        setError(getApiErrorMessage(err, t('landingServiceDetail.loadError')))
      })
      .finally(() => setLoading(false))
  }, [id, safeCategory, t])

  const service = useMemo(() => (rawService ? normalizeService(rawService, images) : null), [rawService, images])
  const backPath = safeCategory ? `/services/${safeCategory}` : '/services'
  const currencyFallback = rawService?.currency ?? 'USD'

  if (loading) {
    return (
      <div className="lp-sheet">
        <DetailSkeleton />
      </div>
    )
  }

  if (!service) {
    return (
      <div className="lp-sheet" style={{ borderColor: 'rgba(220, 100, 100, 0.35)', background: 'rgba(255, 245, 245, 0.9)' }}>
        <p className="font-semibold text-red-900">{error ? t('landingServiceDetail.loadError') : t('landingServiceDetail.notFound')}</p>
        {error ? <p className="mt-1 text-sm text-red-800/90">{error}</p> : null}
        <button type="button" onClick={() => navigate(backPath)} className="lp-link-quiet mt-3 inline-block font-semibold lp-text-accent">
          {t('landingServiceDetail.backBtn')}
        </button>
      </div>
    )
  }

  const categoryLabel = safeCategory ? safeCategory.charAt(0).toUpperCase() + safeCategory.slice(1) : t('landingServiceDetail.breadcrumbRoot')
  const crumbBtn = 'border-0 bg-transparent p-0 font-medium cursor-pointer hover:underline'
  const crumbStyle = { color: 'var(--accent-teal)' } as const

  return (
    <div className="lp-sheet">
      {/* Single breadcrumb — no duplicate back button */}
      <nav className="mb-4 flex items-center gap-1 text-sm lp-muted" aria-label="Breadcrumb">
        <button type="button" onClick={() => navigate('/services')} className={crumbBtn} style={crumbStyle}>
          {t('landingServiceDetail.breadcrumbRoot')}
        </button>
        <span className="mx-1 text-slate-400">/</span>
        <button type="button" onClick={() => navigate(backPath)} className={crumbBtn} style={crumbStyle}>
          {categoryLabel}
        </button>
        <span className="mx-1 text-slate-400">/</span>
        <span style={{ color: 'var(--ink-heading)', fontWeight: 600 }}>{service.name}</span>
      </nav>

      {/* Two-column layout on desktop: content left, sticky booking panel right */}
      <div className="lp-service-detail-layout">
        <div className="lp-service-detail-main">
          <ServiceDetailView service={service} />

          {/* Hotel / resort: room types overview */}
          {(rawService?.type === 'HOTEL' || rawService?.type === 'RESORT') && rooms.filter(r => r.status === 'ACTIVE').length > 0 ? (
            <div className="mt-8 space-y-4">
              <h2 className="text-lg font-semibold lp-text-heading">
                {t('landingBooking.roomTypesHeading')}
              </h2>
              {rooms
                .filter((r) => r.status === 'ACTIVE')
                .map((room) => (
                  <Card key={room.id} surface="landing" className="!p-5">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <h3 className="font-semibold lp-text-heading">{room.roomName}</h3>
                        <p className="mt-0.5 text-sm lp-text-muted">{room.roomType}</p>
                        {room.description ? (
                          <p className="mt-1 text-sm lp-text-muted">{sanitizeText(room.description)}</p>
                        ) : null}
                        {room.capacity ? (
                          <p className="mt-1 text-xs" style={{ color: 'var(--ink-faint)' }}>
                            {t('landingBooking.upToGuests').replace('{n}', String(room.capacity))}
                          </p>
                        ) : null}
                      </div>
                      {room.basePrice != null ? (
                        <div className="shrink-0 text-right">
                          <p className="font-bold text-lg" style={{ color: 'var(--accent-tan-mid)' }}>
                            {room.currency ?? currencyFallback} {room.basePrice.toLocaleString()}
                          </p>
                          <p className="text-xs lp-text-muted">{t('landingBooking.perNight')}</p>
                        </div>
                      ) : null}
                    </div>
                  </Card>
                ))}
            </div>
          ) : null}

          {/* Restaurant menu */}
          {rawService?.type === 'RESTAURANT' && menu ? (
        <div className="mt-8 space-y-6">
          <h2 className="text-lg font-semibold lp-text-heading">
            {t('landingServiceDetail.menuHeading')}
          </h2>
          {menu.sections.length === 0 ? (
            <p className="text-sm lp-muted">{t('landingServiceDetail.menuEmpty')}</p>
          ) : (
            menu.sections.map((section) => (
              <Card key={section.id} surface="landing" className="!p-5">
                <h3 className="text-base font-semibold lp-text-heading">
                  {section.title}
                </h3>
                <ul className="mt-4 divide-y" style={{ borderColor: 'rgba(90, 122, 130, 0.15)' }}>
                  {section.items.map((item) => (
                    <li key={item.id} className="flex gap-3 py-3 first:pt-0">
                      {(() => {
                        const src = safeImageUrl(item.imageUrl)
                        return src ? (
                          <img src={src} alt="" className="h-16 w-16 shrink-0 rounded-md object-cover" loading="lazy" />
                        ) : null
                      })()}
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-baseline justify-between gap-2">
                          <span className="font-medium lp-text-heading">
                            {item.name}
                          </span>
                          {item.price != null ? (
                            <span className="text-sm lp-text-muted">
                              {item.currency ?? currencyFallback} {item.price}
                            </span>
                          ) : null}
                        </div>
                        {item.description ? (
                          <p className="mt-1 text-sm lp-text-muted">
                            {sanitizeText(item.description)}
                          </p>
                        ) : null}
                      </div>
                    </li>
                  ))}
                </ul>
              </Card>
            ))
          )}
        </div>
      ) : null}
          {/* Guest reviews */}
          {reviews.length > 0 ? (
            <div className="mt-8">
              <h2 className="mb-4 text-lg font-semibold lp-text-heading">
                {t('landingReviews.heading')}
              </h2>
              <div className="space-y-4">
                {reviews.slice(0, 6).map((review) => (
                  <div key={review.id} className="rounded-2xl border p-5" style={{ borderColor: 'rgba(90,122,130,0.18)', background: 'rgba(255,255,255,0.55)' }}>
                    <div className="flex items-center justify-between gap-3">
                      <span className="font-medium lp-text-heading">{review.userName || t('landingReviews.anonymous')}</span>
                      <span className="flex items-center gap-0.5 text-sm" style={{ color: 'var(--accent-tan-mid)' }}>
                        {Array.from({ length: 5 }, (_, i) => (
                          <svg key={i} width="13" height="13" viewBox="0 0 20 20" fill={i < (review.rating ?? 0) ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.5" aria-hidden>
                            <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                          </svg>
                        ))}
                      </span>
                    </div>
                    {review.comment ? (
                      <p className="mt-2 text-sm lp-text-muted">{sanitizeText(review.comment)}</p>
                    ) : null}
                    {review.response ? (
                      <div className="mt-3 rounded-xl p-3 text-sm" style={{ background: 'rgba(61,112,128,0.07)', color: 'var(--ink-muted)' }}>
                        <span className="font-medium" style={{ color: 'var(--accent-teal)' }}>{t('landingReviews.providerReply')}: </span>
                        {sanitizeText(review.response)}
                      </div>
                    ) : null}
                    {review.createdAt ? (
                      <p className="mt-2 text-xs" style={{ color: 'var(--ink-faint)' }}>
                        {new Date(review.createdAt).toLocaleDateString()}
                      </p>
                    ) : null}
                  </div>
                ))}
              </div>
            </div>
          ) : null}

          {/* Cancellation policy */}
          {rawService?.policies ? (
            <div className="mt-6 rounded-2xl border p-5" style={{ borderColor: 'rgba(90,122,130,0.2)', background: 'rgba(255,255,255,0.55)' }}>
              <h2 className="text-base font-semibold lp-text-heading mb-2">
                {t('landingBooking.cancelPolicyHeading')}
              </h2>
              <p className="text-sm lp-text-muted">{rawService.policies}</p>
            </div>
          ) : null}

          {/* Map */}
          {rawService?.latitude != null && rawService?.longitude != null ? (
            <div className="mt-8">
              <h2 className="mb-4 text-lg font-semibold lp-text-heading">
                {t('landingBooking.locationHeading')}
              </h2>
              <ProviderMap
                pins={[{
                  id: rawService.id,
                  name: rawService.name,
                  type: rawService.type,
                  latitude: rawService.latitude,
                  longitude: rawService.longitude,
                }]}
                center={[rawService.latitude, rawService.longitude]}
                zoom={13}
                className="h-64 w-full rounded-2xl overflow-hidden"
              />
            </div>
          ) : null}
        </div>{/* end lp-service-detail-main */}

        {/* Sticky booking sidebar */}
        <aside className="lp-service-detail-sidebar">
          <div className="lp-service-booking-sticky">
            <LandingServiceBookingPanel service={service} />
          </div>
        </aside>
      </div>{/* end lp-service-detail-layout */}
    </div>
  )
}
