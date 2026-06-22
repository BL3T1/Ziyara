import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { getApiErrorMessage, mapAPI } from '../../services/api'
import { ProviderMap, type ProviderMapPin } from '../../components/maps/ProviderMap'
import type { ProviderMapPinDto } from '../../types/api'

const ALL_TYPES = ['HOTEL', 'RESORT', 'RESTAURANT', 'TAXI', 'TRIP'] as const
type PinType = typeof ALL_TYPES[number]

function toPin(dto: ProviderMapPinDto): ProviderMapPin {
  return {
    id: dto.id,
    name: dto.name,
    type: dto.type,
    latitude: dto.latitude,
    longitude: dto.longitude,
    status: dto.status,
    thumbnailUrl: dto.thumbnailUrl,
  }
}

export function MapPage() {
  const { t } = useLanguage()
  useDocumentMeta({ title: `${t('nav.map')} — Ziyara` })
  const [allPins, setAllPins] = useState<ProviderMapPin[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeTypes, setActiveTypes] = useState<Set<PinType>>(new Set(ALL_TYPES))

  useEffect(() => {
    setLoading(true)
    mapAPI
      .getPins()
      .then((r) => {
        const data = Array.isArray(r.data) ? r.data : []
        setAllPins(data.map(toPin))
      })
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  function toggleType(type: PinType) {
    setActiveTypes((prev) => {
      const next = new Set(prev)
      next.has(type) ? next.delete(type) : next.add(type)
      return next
    })
  }

  const visiblePins = allPins.filter((p) => activeTypes.has(p.type.toUpperCase() as PinType))

  const TYPE_LABEL: Record<string, string> = {
    HOTEL: t('mapPage.layerHotels'),
    RESORT: t('mapPage.layerResorts'),
    RESTAURANT: t('mapPage.layerRestaurants'),
    TAXI: t('mapPage.layerTaxis'),
    TRIP: t('mapPage.layerTrips'),
  }

  return (
    <>
      <h1 className="app-page-title">{t('mapPage.title')}</h1>

      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
          {error}
        </div>
      )}

      {/* Layer toggles */}
      <div className="flex flex-wrap gap-2">
        {ALL_TYPES.map((type) => (
          <button
            key={type}
            type="button"
            onClick={() => toggleType(type)}
            className={activeTypes.has(type) ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
          >
            {TYPE_LABEL[type] ?? type}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="h-[520px] animate-pulse rounded-xl bg-slate-100 dark:bg-white/[0.04]" />
      ) : (
        <ProviderMap pins={visiblePins} className="h-[520px] w-full rounded-xl overflow-hidden shadow-md" />
      )}

      <p className="text-xs text-slate-400 dark:text-slate-500">
        {t('mapPage.pinCount', { count: visiblePins.length })}
      </p>
    </>
  )
}
