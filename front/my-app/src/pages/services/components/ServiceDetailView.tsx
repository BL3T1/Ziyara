import { useMemo, useState } from 'react'
import { Card } from '../../../components/Card'
import type { ServiceEntity } from '../serviceModel'
import { getServicePrimaryImageUrl } from '../serviceModel'
import { sanitizeText, safeImageUrl } from '../../../utils/safeRendering'

interface ServiceDetailViewProps {
  service: ServiceEntity
}

export function ServiceDetailView({ service }: ServiceDetailViewProps) {
  const [activeImageIndex, setActiveImageIndex] = useState(0)
  const [activeTab, setActiveTab] = useState<'overview' | 'photos' | 'pricing'>('overview')
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
      </div>

      {activeTab === 'overview' ? (
        <Card className="p-6">
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">{service.name}</h1>
          <p className="mt-3 text-slate-600 dark:text-slate-300">
            {sanitizeText(service.description || 'No description available.')}
          </p>
          <div className="mt-4 grid gap-2 text-sm sm:grid-cols-2">
            <p className="text-slate-700 dark:text-slate-200">{sanitizeText(metadata)}</p>
            <p className="text-slate-700 dark:text-slate-200">
              Rating: {service.rating != null ? `${service.rating.toFixed(1)} / 5` : 'No rating yet'}
            </p>
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
    </div>
  )
}
