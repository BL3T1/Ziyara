import type { Dispatch, SetStateAction } from 'react'
import { useLanguage } from '../../context/LanguageContext'

export interface ServiceFilters {
  q: string
  city: string
  minPrice: string
  maxPrice: string
  minRating: string
}

// eslint-disable-next-line react-refresh/only-export-components
export const EMPTY_FILTERS: ServiceFilters = { q: '', city: '', minPrice: '', maxPrice: '', minRating: '' }

interface Props {
  filters: ServiceFilters
  setFilters: Dispatch<SetStateAction<ServiceFilters>>
  cities: string[]
}

export function ServicesFilterBar({ filters, setFilters, cities }: Props) {
  const { t } = useLanguage()
  const set = (key: keyof ServiceFilters) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setFilters((f) => ({ ...f, [key]: e.target.value }))

  const hasActive = Object.values(filters).some(Boolean)

  return (
    <div className="mt-5 flex flex-wrap items-end gap-3">
      {/* Search */}
      <div className="flex-1 min-w-[180px]">
        <label className="lp-label mb-1 block text-xs">{t('landingFilter.searchLabel')}</label>
        <input
          type="search"
          value={filters.q}
          onChange={set('q')}
          placeholder={t('landingFilter.searchPlaceholder')}
          className="lp-input w-full"
        />
      </div>

      {/* City */}
      {cities.length > 0 && (
        <div className="min-w-[130px]">
          <label className="lp-label mb-1 block text-xs">{t('landingFilter.cityLabel')}</label>
          <select value={filters.city} onChange={set('city')} className="lp-input w-full">
            <option value="">{t('landingFilter.cityAll')}</option>
            {cities.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>
      )}

      {/* Price range */}
      <div className="min-w-[90px]">
        <label className="lp-label mb-1 block text-xs">{t('landingFilter.minPriceLabel')}</label>
        <input type="number" min="0" value={filters.minPrice} onChange={set('minPrice')} placeholder="0" className="lp-input w-full" />
      </div>
      <div className="min-w-[90px]">
        <label className="lp-label mb-1 block text-xs">{t('landingFilter.maxPriceLabel')}</label>
        <input type="number" min="0" value={filters.maxPrice} onChange={set('maxPrice')} placeholder="—" className="lp-input w-full" />
      </div>

      {/* Min rating */}
      <div className="min-w-[110px]">
        <label className="lp-label mb-1 block text-xs">{t('landingFilter.minRatingLabel')}</label>
        <select value={filters.minRating} onChange={set('minRating')} className="lp-input w-full">
          <option value="">{t('landingFilter.ratingAny')}</option>
          {[1, 2, 3, 4, 5].map((n) => (
            <option key={n} value={n}>{'★'.repeat(n)}</option>
          ))}
        </select>
      </div>

      {/* Clear */}
      {hasActive && (
        <button
          type="button"
          onClick={() => setFilters(EMPTY_FILTERS)}
          className="lp-btn lp-btn-outline lp-btn-sm self-end"
        >
          {t('landingFilter.clearFilters')}
        </button>
      )}
    </div>
  )
}
