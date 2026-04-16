/**
 * Create or edit a provider-owned listing (portal services API).
 */

import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, portalAPI, providersAPI, servicesAPI } from '../../services/api'
import { PARTNER_SERVICE_TYPE_VALUES, type ServiceDto, type ServiceStatusDto, type ServiceTypeDto } from '../../types/api'
import { Card } from '../../components/Card'

const SERVICE_STATUSES: ServiceStatusDto[] = [
  'ACTIVE',
  'INACTIVE',
  'PENDING_APPROVAL',
  'AVAILABLE',
  'UNAVAILABLE',
  'MAINTENANCE',
  'HIDDEN',
  'SUSPENDED',
  'DISCONTINUED',
]

function num(v: string): number | undefined {
  const n = parseFloat(v.replace(/,/g, ''))
  return Number.isFinite(n) ? n : undefined
}

function int(v: string): number | undefined {
  const n = parseInt(v, 10)
  return Number.isFinite(n) ? n : undefined
}

export function PortalListingFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { t } = useLanguage()
  const isNew = id === 'new'

  const [loading, setLoading] = useState(!isNew)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [providerId, setProviderId] = useState<string | null>(null)

  const [type, setType] = useState<ServiceTypeDto>('HOTEL')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [city, setCity] = useState('')
  const [country, setCountry] = useState('')
  const [address, setAddress] = useState('')
  const [basePrice, setBasePrice] = useState('')
  const [currency, setCurrency] = useState('USD')
  const [status, setStatus] = useState<ServiceStatusDto>('PENDING_APPROVAL')
  const [maxGuests, setMaxGuests] = useState('')
  const [totalRooms, setTotalRooms] = useState('')
  const [availableRooms, setAvailableRooms] = useState('')
  const [starRating, setStarRating] = useState('')

  useEffect(() => {
    if (isNew) {
      providersAPI
        .getMe()
        .then((res) => setProviderId(res.data.id))
        .catch(() => setProviderId(null))
      return
    }
    if (!id) return
    setLoading(true)
    setError(null)
    servicesAPI
      .get(id)
      .then((res) => {
        const s = res.data as ServiceDto
        setType(s.type)
        setName(s.name ?? '')
        setDescription(s.description ?? '')
        setCity(s.city ?? '')
        setCountry(s.country ?? '')
        setAddress(s.address ?? '')
        setBasePrice(s.basePrice != null ? String(s.basePrice) : '')
        setCurrency(s.currency ?? 'USD')
        setStatus((s.status as ServiceStatusDto) ?? 'PENDING_APPROVAL')
        setMaxGuests(s.maxGuests != null ? String(s.maxGuests) : '')
        setTotalRooms(s.totalRooms != null ? String(s.totalRooms) : '')
        setAvailableRooms(s.availableRooms != null ? String(s.availableRooms) : '')
        setStarRating(s.starRating != null ? String(s.starRating) : '')
      })
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [id, isNew])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    const price = num(basePrice)
    if (price == null || price < 0) {
      setError(t('portalPages.priceRequired'))
      return
    }
    if (!name.trim()) {
      setError(t('portalPages.nameRequired'))
      return
    }

    setSaving(true)
    try {
      if (isNew) {
        if (!providerId) {
          setError(t('portalPages.providerProfileMissing'))
          setSaving(false)
          return
        }
        const created = await portalAPI.createService({
          providerId,
          type,
          name: name.trim(),
          description: description.trim() || undefined,
          city: city.trim() || undefined,
          country: country.trim() || undefined,
          address: address.trim() || undefined,
          basePrice: price,
          currency: currency.trim() || 'USD',
          maxGuests: int(maxGuests),
          totalRooms: int(totalRooms),
          availableRooms: int(availableRooms),
          starRating: int(starRating),
        })
        const row = created.data as ServiceDto
        navigate(`/portal/listings/${row.id}`, { replace: true })
      } else if (id) {
        await portalAPI.updateService(id, {
          name: name.trim(),
          description: description.trim() || undefined,
          city: city.trim() || undefined,
          country: country.trim() || undefined,
          address: address.trim() || undefined,
          basePrice: price,
          status,
          maxGuests: int(maxGuests),
          totalRooms: int(totalRooms),
          availableRooms: int(availableRooms),
          starRating: int(starRating),
        })
        navigate('/portal/listings')
      }
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  if (!id) {
    return <p className="text-slate-600 dark:text-slate-300">{t('portalPages.invalidRoute')}</p>
  }

  return (
    <>
      <div className="mb-6">
        <Link to="/portal/listings" className="text-sm font-medium text-primary hover:underline">
          ← {t('portalPages.backToListings')}
        </Link>
        <h1 className="mt-3 app-page-title">
          {isNew ? t('portalPages.listingNewTitle') : t('portalPages.listingEditTitle')}
        </h1>
      </div>

      {error && (
        <p className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
          {error}
        </p>
      )}

      {loading ? (
        <p className="text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
      ) : (
        <Card className="p-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            {isNew && (
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldType')}</label>
                <select
                  value={type}
                  onChange={(ev) => setType(ev.target.value as ServiceTypeDto)}
                  className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
                >
                  {PARTNER_SERVICE_TYPE_VALUES.map((x) => (
                    <option key={x} value={x}>
                      {x}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldName')}</label>
              <input
                value={name}
                onChange={(ev) => setName(ev.target.value)}
                required
                className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldDescription')}</label>
              <textarea
                value={description}
                onChange={(ev) => setDescription(ev.target.value)}
                rows={3}
                className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldCity')}</label>
                <input
                  value={city}
                  onChange={(ev) => setCity(ev.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldCountry')}</label>
                <input
                  value={country}
                  onChange={(ev) => setCountry(ev.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldAddress')}</label>
              <input
                value={address}
                onChange={(ev) => setAddress(ev.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldBasePrice')}</label>
                <input
                  value={basePrice}
                  onChange={(ev) => setBasePrice(ev.target.value)}
                  inputMode="decimal"
                  className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldCurrency')}</label>
                <input
                  value={currency}
                  onChange={(ev) => setCurrency(ev.target.value)}
                  disabled={!isNew}
                  title={isNew ? undefined : t('portalPages.currencyLockedHint')}
                  className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 disabled:opacity-60 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
                />
              </div>
            </div>

            {!isNew && (
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldStatus')}</label>
                <select
                  value={status}
                  onChange={(ev) => setStatus(ev.target.value as ServiceStatusDto)}
                  className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
                >
                  {SERVICE_STATUSES.map((x) => (
                    <option key={x} value={x}>
                      {x}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div className="grid gap-4 sm:grid-cols-3">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldMaxGuests')}</label>
                <input
                  value={maxGuests}
                  onChange={(ev) => setMaxGuests(ev.target.value)}
                  inputMode="numeric"
                  className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldTotalRooms')}</label>
                <input
                  value={totalRooms}
                  onChange={(ev) => setTotalRooms(ev.target.value)}
                  inputMode="numeric"
                  className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldAvailableRooms')}</label>
                <input
                  value={availableRooms}
                  onChange={(ev) => setAvailableRooms(ev.target.value)}
                  inputMode="numeric"
                  className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldStarRating')}</label>
              <input
                value={starRating}
                onChange={(ev) => setStarRating(ev.target.value)}
                inputMode="numeric"
                className="mt-1 max-w-xs rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
              />
            </div>

            <div className="flex flex-wrap gap-3 pt-2">
              <button
                type="submit"
                disabled={saving}
                className="dashboard-btn-primary disabled:opacity-50"
              >
                {saving ? t('portalPages.saving') : t('portalPages.save')}
              </button>
              <Link
                to="/portal/listings"
                className="inline-flex items-center rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-200"
              >
                {t('ui.cancel')}
              </Link>
            </div>
          </form>
        </Card>
      )}
    </>
  )
}
