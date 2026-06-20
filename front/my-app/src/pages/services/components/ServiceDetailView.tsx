import { useEffect, useMemo, useState } from 'react'
import { Card } from '../../../components/Card'
import type { ServiceEntity } from '../serviceModel'
import { getServicePrimaryImageUrl } from '../serviceModel'
import { sanitizeText, safeImageUrl } from '../../../utils/safeRendering'
import { reviewsAPI } from '../../../services/api'
import type { ReviewDto } from '../../../types/api'

function StarRow({ rating, max = 5 }: { rating: number; max?: number }) {
  return (
    <span className="inline-flex gap-px" aria-label={`${rating} out of ${max} stars`}>
      {Array.from({ length: max }).map((_, i) => (
        <svg key={i} className={`h-4 w-4 ${i < Math.round(rating) ? 'text-amber-400' : 'text-slate-200'}`} fill="currentColor" viewBox="0 0 20 20">
          <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
        </svg>
      ))}
    </span>
  )
}

interface ServiceDetailViewProps {
  service: ServiceEntity
}

export function ServiceDetailView({ service }: ServiceDetailViewProps) {
  const [activeImageIndex, setActiveImageIndex] = useState(0)
  const [activeTab, setActiveTab] = useState<'overview' | 'photos' | 'pricing' | 'reviews'>('overview')
  const [reviews, setReviews] = useState<ReviewDto[]>([])
  const [reviewsLoading, setReviewsLoading] = useState(false)
  const [showAllReviews, setShowAllReviews] = useState(false)

  useEffect(() => {
    setReviewsLoading(true)
    reviewsAPI.getServiceReviews(service.id)
      .then((r) => setReviews(Array.isArray(r.data) ? r.data : []))
      .catch(() => setReviews([]))
      .finally(() => setReviewsLoading(false))
  }, [service.id])

  const avgRating = reviews.length ? reviews.reduce((s, r) => s + r.rating, 0) / reviews.length : null
  const images = useMemo(
    () => (service.images.length ? service.images : [{ id: 'fallback', url: getServicePrimaryImageUrl(service), primary: true, displayOrder: 0, category: 'OTHER', serviceId: service.id }]),
    [service],
  )
  const activeImage = images[activeImageIndex] ?? images[0]

  const metadata = (() => {
    if (service.category === 'hotels' || service.category === 'resorts')
      return `Amenities: ${service.metadata.amenities.join(', ')}`
    if (service.category === 'restaurants') return `Cuisine: ${service.metadata.cuisine}`
    if (service.category === 'taxis') return `Vehicle type: ${service.metadata.vehicleType}`
    return `Duration: ${service.metadata.duration}`
  })()

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => setActiveTab('overview')}
          className={`rounded-lg border-2 px-3 py-1.5 text-sm font-medium ${
            activeTab === 'overview'
              ? 'border-[#163d56] bg-[#1e4d6b] text-white shadow-sm'
              : 'border-slate-300 bg-white text-slate-800 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200'
          }`}
        >
          Overview
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('photos')}
          className={`rounded-lg border-2 px-3 py-1.5 text-sm font-medium ${
            activeTab === 'photos'
              ? 'border-[#163d56] bg-[#1e4d6b] text-white shadow-sm'
              : 'border-slate-300 bg-white text-slate-800 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200'
          }`}
        >
          Photos
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('pricing')}
          className={`rounded-lg border-2 px-3 py-1.5 text-sm font-medium ${
            activeTab === 'pricing'
              ? 'border-[#163d56] bg-[#1e4d6b] text-white shadow-sm'
              : 'border-slate-300 bg-white text-slate-800 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200'
          }`}
        >
          Pricing
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('reviews')}
          className={`rounded-lg border-2 px-3 py-1.5 text-sm font-medium ${
            activeTab === 'reviews'
              ? 'border-[#163d56] bg-[#1e4d6b] text-white shadow-sm'
              : 'border-slate-300 bg-white text-slate-800 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200'
          }`}
        >
          Reviews {reviews.length > 0 && `(${reviews.length})`}
        </button>
      </div>

      {activeTab === 'overview' ? (
        <Card className="p-6">
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">{service.name}</h1>
          <p className="mt-3 text-slate-600 dark:text-slate-300">
            {sanitizeText(service.description || 'No description available.')}
          </p>
          <div className="mt-4 grid gap-2 text-sm sm:grid-cols-2">
            <p className="text-slate-700 dark:text-slate-200">{sanitizeText(metadata)}</p>
            <div className="flex items-center gap-2 text-slate-700 dark:text-slate-200">
              {reviewsLoading ? (
                <span className="text-sm text-slate-400">Loading ratings…</span>
              ) : avgRating != null ? (
                <>
                  <StarRow rating={avgRating} />
                  <span className="text-sm font-semibold">{avgRating.toFixed(1)}</span>
                  <span className="text-xs text-slate-400">({reviews.length} review{reviews.length !== 1 ? 's' : ''})</span>
                </>
              ) : (
                <span className="text-sm text-slate-400">No reviews yet</span>
              )}
            </div>
          </div>
        </Card>
      ) : null}

      {activeTab === 'photos' ? (
        <Card className="p-4">
          <div className="overflow-hidden rounded-xl bg-slate-200 dark:bg-slate-800">
            <img
              src={safeImageUrl(activeImage.url) ?? '/default-avatar.svg'}
              alt={service.name}
              className="h-72 w-full object-cover"
            />
          </div>
          <div className="mt-3 flex gap-2 overflow-x-auto">
            {images.map((img, index) => (
              <button
                key={img.id}
                type="button"
                onClick={() => setActiveImageIndex(index)}
                className={`overflow-hidden rounded-lg border ${index === activeImageIndex ? 'border-primary' : 'border-slate-300 dark:border-slate-700'}`}
              >
                <img
                  src={safeImageUrl(img.url) ?? '/default-avatar.svg'}
                  alt=""
                  className="h-16 w-24 object-cover"
                  loading="lazy"
                />
              </button>
            ))}
          </div>
        </Card>
      ) : null}

      {activeTab === 'pricing' ? (
        <Card className="p-6">
          <h2 className="text-xl font-semibold text-slate-900 dark:text-slate-100">Pricing</h2>
          <p className="mt-3 text-slate-700 dark:text-slate-200">
            {service.price != null ? `${service.currency} ${service.price}` : 'Price not specified'}
          </p>
          <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">
            Final price may vary based on seasonality, availability, and booking policies.
          </p>
        </Card>
      ) : null}

      {activeTab === 'reviews' ? (
        <Card className="p-6">
          {avgRating != null && (
            <div className="mb-5 flex items-center gap-3">
              <span className="text-4xl font-bold text-slate-900 dark:text-slate-100">{avgRating.toFixed(1)}</span>
              <div>
                <StarRow rating={avgRating} />
                <p className="mt-0.5 text-xs text-slate-500">{reviews.length} review{reviews.length !== 1 ? 's' : ''}</p>
              </div>
            </div>
          )}
          {reviewsLoading ? (
            <p className="text-sm text-slate-400">Loading reviews…</p>
          ) : reviews.length === 0 ? (
            <p className="text-sm text-slate-500">No reviews yet.</p>
          ) : (
            <div className="divide-y divide-slate-100 dark:divide-slate-700">
              {(showAllReviews ? reviews : reviews.slice(0, 5))
                .sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? ''))
                .map((r) => (
                  <div key={r.id} className="py-4">
                    <div className="flex items-center gap-2">
                      <StarRow rating={r.rating} />
                      <span className="text-sm font-medium text-slate-700 dark:text-slate-300">{r.userName ?? 'Guest'}</span>
                      {r.createdAt && (
                        <span className="ms-auto text-xs text-slate-400">
                          {new Date(r.createdAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })}
                        </span>
                      )}
                    </div>
                    {r.comment && <p className="mt-1.5 text-sm text-slate-600 dark:text-slate-300">{sanitizeText(r.comment)}</p>}
                    {r.response && (
                      <div className="mt-2 rounded-lg bg-slate-50 px-3 py-2 dark:bg-slate-800">
                        <p className="text-xs font-semibold text-slate-500">Provider response</p>
                        <p className="mt-0.5 text-sm text-slate-600 dark:text-slate-300">{sanitizeText(r.response)}</p>
                      </div>
                    )}
                  </div>
                ))}
              {reviews.length > 5 && (
                <button
                  type="button"
                  onClick={() => setShowAllReviews((v) => !v)}
                  className="mt-3 text-sm font-medium text-[#1e4d6b] hover:underline dark:text-sky-400"
                >
                  {showAllReviews ? 'Show fewer' : `Show all ${reviews.length} reviews`}
                </button>
              )}
            </div>
          )}
        </Card>
      ) : null}
    </div>
  )
}
