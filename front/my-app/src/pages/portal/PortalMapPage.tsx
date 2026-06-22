import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { getApiErrorMessage, mapAPI } from '../../services/api'
import { ProviderMap, type ProviderMapPin } from '../../components/maps/ProviderMap'
import type { ProviderMapPinDto } from '../../types/api'

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

export function PortalMapPage() {
  const { t } = useLanguage()
  useDocumentMeta({ title: `${t('nav.portal_map')} — Ziyara` })
  const [pins, setPins] = useState<ProviderMapPin[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    mapAPI
      .getPortalPins()
      .then((r) => {
        const data = Array.isArray(r.data) ? r.data : []
        setPins(data.map(toPin))
      })
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  return (
    <>
      <h1 className="app-page-title">{t('mapPage.title')}</h1>
      <p className="text-sm text-slate-500 dark:text-slate-400">{t('mapPage.portalHint')}</p>

      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
          {error}
        </div>
      )}

      {loading ? (
        <div className="h-[520px] animate-pulse rounded-xl bg-slate-100 dark:bg-white/[0.04]" />
      ) : pins.length === 0 ? (
        <div className="dashboard-empty-state">
          <div className="dashboard-empty-state__icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
              <polygon points="3,6 9,3 15,6 21,3 21,18 15,21 9,18 3,21" />
              <line x1="9" x2="9" y1="3" y2="18" />
              <line x1="15" x2="15" y1="6" y2="21" />
            </svg>
          </div>
          <p className="dashboard-empty-state__title">{t('mapPage.noPins')}</p>
          <p className="dashboard-empty-state__body">{t('mapPage.noPinsHint')}</p>
        </div>
      ) : (
        <ProviderMap pins={pins} className="h-[520px] w-full rounded-xl overflow-hidden shadow-md" />
      )}

      {pins.length > 0 && (
        <p className="text-xs text-slate-400 dark:text-slate-500">
          {t('mapPage.pinCount', { count: pins.length })}
        </p>
      )}
    </>
  )
}
