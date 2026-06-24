import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
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

  const isHotelOrResort = rawService?.type === 'HOTEL' || rawService?.type === 'RESORT'
  const isRestaurantType = rawService?.type === 'RESTAURANT'
  const activeRooms = rooms.filter((r) => r.status === 'ACTIVE')
  const menuHasSections = (menu?.sections?.length ?? 0) > 0

  type DetailTab = 'overview' | 'rooms' | 'menu' | 'reviews' | 'location'
  const [activeTab, setActiveTab] = useState<DetailTab>('overview')

  const tabItems: { key: DetailTab; label: string }[] = [
    { key: 'overview', label: t('landingServiceDetail.tabOverview') || 'Overview' },
    ...(isHotelOrResort && activeRooms.length > 0 ? [{ key: 'rooms' as DetailTab, label: t('landingBooking.roomTypesHeading') || 'Rooms' }] : []),
    ...(isRestaurantType && menuHasSections ? [{ key: 'menu' as DetailTab, label: t('landingServiceDetail.menuHeading') || 'Menu' }] : []),
    ...(reviews.length > 0 ? [{ key: 'reviews' as DetailTab, label: t('landingReviews.heading') || 'Reviews' }] : []),
    ...(rawService?.latitude != null ? [{ key: 'location' as DetailTab, label: t('landingBooking.locationHeading') || 'Location' }] : []),
  ]

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

  const mainImageSrc = safeImageUrl(images[0]?.imageUrl ?? (service as any)?.imageUrl)

  return (
    <div className="max-w-container-max mx-auto px-4 md:px-margin-desktop pb-section-gap">
      {/* Breadcrumb */}
      <nav className="mb-6 flex items-center gap-1.5 text-sm text-on-surface-variant" aria-label="Breadcrumb">
        <button type="button" onClick={() => navigate('/services')} className="hover:text-stitch-primary transition-colors bg-transparent border-0 p-0 cursor-pointer font-medium">
          {t('landingServiceDetail.breadcrumbRoot')}
        </button>
        <span className="text-outline">/</span>
        <button type="button" onClick={() => navigate(backPath)} className="hover:text-stitch-primary transition-colors bg-transparent border-0 p-0 cursor-pointer font-medium">
          {categoryLabel}
        </button>
        <span className="text-outline">/</span>
        <span className="text-on-surface font-semibold truncate max-w-[200px]">{service.name}</span>
      </nav>

      {/* Image gallery */}
      {mainImageSrc && (
        <div className="mb-6">
          <img
            src={mainImageSrc}
            alt={sanitizeText(service.name)}
            className="w-full h-[320px] md:h-[400px] object-cover rounded-xl shadow-sm"
          />
          {images.length > 1 && (
            <div className="grid grid-cols-4 gap-stack-sm mt-stack-sm">
              {images.slice(1, 5).map((img, i) => {
                const src = safeImageUrl(img.imageUrl)
                const isLast = i === 3 && images.length > 5
                return src ? (
                  <div key={img.id ?? i} className="relative rounded-lg overflow-hidden">
                    <img src={src} alt="" className={`w-full h-20 md:h-24 object-cover ${isLast ? 'opacity-50' : 'cursor-pointer hover:opacity-80 transition-opacity'}`} loading="lazy" />
                    {isLast && (
                      <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
                        <span className="text-white font-label-md font-bold text-sm">+{images.length - 5} photos</span>
                      </div>
                    )}
                  </div>
                ) : null
              })}
            </div>
          )}
        </div>
      )}

      {/* 12-col grid: content (8) + booking panel (4) */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-gutter items-start">

        {/* ── Left content column ── */}
        <div className="md:col-span-8 flex flex-col gap-stack-lg">

          {/* Service header */}
          <div>
            <h1 className="font-headline-lg text-headline-lg text-on-surface">{sanitizeText(service.name)}</h1>
            {rawService?.city && (
              <div className="flex items-center gap-1.5 mt-2 text-on-surface-variant">
                <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" className="text-stitch-primary shrink-0" aria-hidden>
                  <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"/><circle cx="12" cy="9" r="2.5"/>
                </svg>
                <span className="font-body-md text-body-md">{sanitizeText(rawService.city)}</span>
              </div>
            )}
          </div>

          {/* Tabs */}
          {tabItems.length > 1 && (
            <div className="flex gap-0 border-b border-outline-variant/30 -mb-4 overflow-x-auto">
              {tabItems.map((tab) => (
                <button
                  key={tab.key}
                  type="button"
                  onClick={() => setActiveTab(tab.key)}
                  className={`shrink-0 px-4 py-3 font-label-md text-label-md font-semibold border-b-2 transition-colors whitespace-nowrap ${
                    activeTab === tab.key
                      ? 'text-stitch-primary border-stitch-primary'
                      : 'text-on-surface-variant border-transparent hover:text-stitch-primary'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          )}

          {/* Tab content */}
          {(activeTab === 'overview' || tabItems.length === 1) && (
            <div className="flex flex-col gap-6">
              <ServiceDetailView service={service} />
              {rawService?.policies && (
                <div className="bg-surface-container-lowest rounded-xl border border-outline-variant/20 p-5">
                  <h3 className="font-headline-md text-headline-md text-on-surface mb-2">
                    {t('landingBooking.cancelPolicyHeading')}
                  </h3>
                  <p className="font-body-md text-body-md text-on-surface-variant">{rawService.policies}</p>
                </div>
              )}
            </div>
          )}

          {/* Rooms tab */}
          {activeTab === 'rooms' && isHotelOrResort && (
            <div className="space-y-4">
              {activeRooms.map((room) => (
                <div key={room.id} className="bg-surface-container-lowest rounded-xl border border-outline-variant/20 p-5 flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <h3 className="font-headline-md text-headline-md text-on-surface">{room.roomName}</h3>
                    <p className="font-body-md text-body-md text-on-surface-variant text-sm mt-0.5">{room.roomType}</p>
                    {room.description && (
                      <p className="font-body-md text-body-md text-on-surface-variant text-sm mt-1">{sanitizeText(room.description)}</p>
                    )}
                    {room.capacity && (
                      <p className="text-xs text-on-surface-variant mt-1">
                        {t('landingBooking.upToGuests').replace('{n}', String(room.capacity))}
                      </p>
                    )}
                  </div>
                  {room.basePrice != null && (
                    <div className="shrink-0 text-right">
                      <p className="font-bold text-lg text-primary-container">
                        {room.currency ?? currencyFallback} {room.basePrice.toLocaleString()}
                      </p>
                      <p className="text-xs text-on-surface-variant">{t('landingBooking.perNight')}</p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}

          {/* Menu tab */}
          {activeTab === 'menu' && isRestaurantType && menu && (
            <div className="space-y-6">
              {menu.sections.length === 0 ? (
                <p className="font-body-md text-body-md text-on-surface-variant">{t('landingServiceDetail.menuEmpty')}</p>
              ) : (
                menu.sections.map((section) => (
                  <div key={section.id} className="bg-surface-container-lowest rounded-xl border border-outline-variant/20 p-5">
                    <h3 className="font-headline-md text-headline-md text-on-surface mb-4">{section.title}</h3>
                    <ul className="divide-y divide-outline-variant/20">
                      {section.items.map((item) => {
                        const src = safeImageUrl(item.imageUrl)
                        return (
                          <li key={item.id} className="flex gap-3 py-3 first:pt-0">
                            {src && <img src={src} alt="" className="h-16 w-16 shrink-0 rounded-lg object-cover" loading="lazy" />}
                            <div className="min-w-0 flex-1">
                              <div className="flex flex-wrap items-baseline justify-between gap-2">
                                <span className="font-headline-md text-headline-md text-on-surface text-base">{item.name}</span>
                                {item.price != null && (
                                  <span className="font-label-md text-label-md text-primary-container font-bold">
                                    {item.currency ?? currencyFallback} {item.price}
                                  </span>
                                )}
                              </div>
                              {item.description && (
                                <p className="font-body-md text-body-md text-on-surface-variant text-sm mt-1">{sanitizeText(item.description)}</p>
                              )}
                            </div>
                          </li>
                        )
                      })}
                    </ul>
                  </div>
                ))
              )}
            </div>
          )}

          {/* Reviews tab */}
          {activeTab === 'reviews' && reviews.length > 0 && (
            <div className="space-y-4">
              {reviews.slice(0, 8).map((review) => (
                <div key={review.id} className="bg-surface-container-lowest rounded-xl border border-outline-variant/20 p-5">
                  <div className="flex items-center justify-between gap-3 mb-2">
                    <span className="font-headline-md text-headline-md text-on-surface text-base font-semibold">
                      {review.userName || t('landingReviews.anonymous')}
                    </span>
                    <span className="flex items-center gap-0.5 text-primary-container">
                      {Array.from({ length: 5 }, (_, i) => (
                        <svg key={i} width="13" height="13" viewBox="0 0 20 20" fill={i < (review.rating ?? 0) ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.5" aria-hidden>
                          <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                        </svg>
                      ))}
                    </span>
                  </div>
                  {review.comment && (
                    <p className="font-body-md text-body-md text-on-surface-variant">{sanitizeText(review.comment)}</p>
                  )}
                  {review.response && (
                    <div className="mt-3 rounded-lg bg-surface-container p-3 border border-outline-variant/20">
                      <span className="font-label-md text-label-md text-secondary font-semibold">{t('landingReviews.providerReply')}: </span>
                      <span className="font-body-md text-body-md text-on-surface-variant">{sanitizeText(review.response)}</span>
                    </div>
                  )}
                  {review.createdAt && (
                    <p className="text-xs text-on-surface-variant mt-2">{new Date(review.createdAt).toLocaleDateString()}</p>
                  )}
                </div>
              ))}
            </div>
          )}

          {/* Location tab */}
          {activeTab === 'location' && rawService?.latitude != null && rawService?.longitude != null && (
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
              className="h-72 w-full rounded-xl overflow-hidden border border-outline-variant/20"
            />
          )}
        </div>

        {/* ── Sticky booking panel (4 cols) ── */}
        <div className="md:col-span-4">
          <div className="sticky top-28 bg-surface-container-lowest rounded-xl shadow-lg border border-outline-variant/20 overflow-hidden">
            {/* Price header */}
            <div className="px-6 pt-6 pb-2 border-b border-outline-variant/10">
              {rawService?.price != null ? (
                <div className="flex items-baseline gap-2">
                  <span className="font-headline-lg text-headline-lg text-primary-container font-bold">
                    {currencyFallback} {Number(rawService.price).toLocaleString()}
                  </span>
                  {isHotelOrResort && (
                    <span className="font-body-md text-body-md text-on-surface-variant">/ {t('landingBooking.perNight') || 'night'}</span>
                  )}
                </div>
              ) : (
                <span className="font-headline-md text-headline-md text-on-surface-variant">{t('landingBooking.priceOnRequest')}</span>
              )}
              {service.rating != null && (
                <div className="flex items-center gap-1 mt-1">
                  <svg width="14" height="14" viewBox="0 0 20 20" fill="currentColor" className="text-primary-container" aria-hidden>
                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                  </svg>
                  <span className="font-label-md text-label-md text-on-surface font-semibold">{service.rating.toFixed(1)}</span>
                  {reviews.length > 0 && <span className="text-xs text-on-surface-variant">({reviews.length} reviews)</span>}
                </div>
              )}
            </div>
            <div className="px-6 pb-6">
              <LandingServiceBookingPanel service={service} />
            </div>
          </div>
        </div>

      </div>
    </div>
  )
}
