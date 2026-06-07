/**
 * Create or edit a provider-owned listing.
 * Service type is fixed to the provider's type — no manual selection.
 * Form sections are rendered conditionally based on service type.
 */

import { useEffect, useState } from 'react'
import { Link, useBlocker, useNavigate, useParams } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, portalAPI, providersAPI, servicesAPI } from '../../services/api'
import type { ServiceDto, ServiceStatusDto, ServiceTypeDto } from '../../types/api'
import { Card } from '../../components/Card'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { portalListingSchema } from '../../lib/validation'

const SERVICE_STATUSES: ServiceStatusDto[] = ['ACTIVE', 'INACTIVE']

const HOTEL_AMENITIES = [
  'wifi', 'pool', 'gym', 'spa', 'parking', 'restaurant',
  'airportShuttle', 'roomService', 'airConditioning', 'businessCenter',
  'breakfast', 'petFriendly',
] as const
type HotelAmenity = typeof HOTEL_AMENITIES[number]

const VEHICLE_TYPES = ['sedan', 'suv', 'van', 'bus', 'limousine'] as const

function num(v: string): number | undefined {
  const n = parseFloat(v.replace(/,/g, ''))
  return Number.isFinite(n) ? n : undefined
}

function int(v: string): number | undefined {
  const n = parseInt(v, 10)
  return Number.isFinite(n) ? n : undefined
}

function SectionHeader({ label }: { label: string }) {
  return (
    <div className="border-t border-slate-200 pt-5 dark:border-slate-700">
      <p className="text-xs font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500">
        {label}
      </p>
    </div>
  )
}

export function PortalListingFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { t } = useLanguage()
  const isNew = id === 'new'

  const [loading, setLoading] = useState(!isNew)
  const [saving, setSaving] = useState(false)
  const [isDirty, setIsDirty] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<{
    name?: string; description?: string; basePrice?: string; currency?: string
  }>({})
  const [providerId, setProviderId] = useState<string | null>(null)

  // ── Core fields ──────────────────────────────────────────────────────────
  const [type, setType] = useState<ServiceTypeDto>('HOTEL')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [city, setCity] = useState('')
  const [country, setCountry] = useState('')
  const [address, setAddress] = useState('')
  const [basePrice, setBasePrice] = useState('')
  const [currency, setCurrency] = useState('USD')
  const [status, setStatus] = useState<ServiceStatusDto>('ACTIVE')
  const [maxGuests, setMaxGuests] = useState('')
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [policies, setPolicies] = useState('')

  // ── HOTEL / RESORT ────────────────────────────────────────────────────────
  const [totalRooms, setTotalRooms] = useState('')
  const [availableRooms, setAvailableRooms] = useState('')
  const [starRating, setStarRating] = useState('')
  const [checkInTime, setCheckInTime] = useState('14:00')
  const [checkOutTime, setCheckOutTime] = useState('11:00')
  const [selectedAmenities, setSelectedAmenities] = useState<Set<HotelAmenity>>(new Set())

  // ── RESTAURANT ────────────────────────────────────────────────────────────
  const [cuisineType, setCuisineType] = useState('')
  const [openingHours, setOpeningHours] = useState('')
  const [dressCode, setDressCode] = useState('')

  // ── TRIP ──────────────────────────────────────────────────────────────────
  const [durationDays, setDurationDays] = useState('')
  const [includes, setIncludes] = useState('')
  const [excludes, setExcludes] = useState('')

  // ── TAXI ──────────────────────────────────────────────────────────────────
  const [vehicleType, setVehicleType] = useState('')
  const [serviceArea, setServiceArea] = useState('')

  // ── Type flags ────────────────────────────────────────────────────────────
  const isHotelOrResort = type === 'HOTEL' || type === 'RESORT'
  const isTaxi = type === 'TAXI'
  const isRestaurant = type === 'RESTAURANT'
  const isTrip = type === 'TRIP'

  const maxGuestsLabel = isTaxi
    ? t('portalPages.fieldPassengerCapacity')
    : isRestaurant
      ? t('portalPages.fieldSeatingCapacity')
      : isTrip
        ? t('portalPages.fieldMaxParticipants')
        : t('portalPages.fieldMaxGuests')

  // ── Load existing service ──────────────────────────────────────────────────
  useEffect(() => {
    if (isNew) {
      providersAPI.getMe().then((res) => {
        setProviderId(res.data.id)
        if (res.data.type) setType(res.data.type as ServiceTypeDto)
      }).catch(() => setProviderId(null))
      return
    }
    if (!id) return
    setLoading(true)
    setError(null)
    servicesAPI.get(id).then((res) => {
      const s = res.data as ServiceDto
      setType(s.type)
      setName(s.name ?? '')
      setDescription(s.description ?? '')
      setCity(s.city ?? '')
      setCountry(s.country ?? '')
      setAddress(s.address ?? '')
      setBasePrice(s.basePrice != null ? String(s.basePrice) : '')
      setCurrency(s.currency ?? 'USD')
      setStatus((s.status as ServiceStatusDto) ?? 'ACTIVE')
      setMaxGuests(s.maxGuests != null ? String(s.maxGuests) : '')
      setLatitude(s.latitude != null ? String(s.latitude) : '')
      setLongitude(s.longitude != null ? String(s.longitude) : '')
      setPolicies(s.policies ?? '')
      setTotalRooms(s.totalRooms != null ? String(s.totalRooms) : '')
      setAvailableRooms(s.availableRooms != null ? String(s.availableRooms) : '')
      setStarRating(s.starRating != null ? String(s.starRating) : '')
      if (s.checkInTime) setCheckInTime(s.checkInTime.substring(0, 5))
      if (s.checkOutTime) setCheckOutTime(s.checkOutTime.substring(0, 5))

      if (s.amenities) {
        const selected = (Object.entries(s.amenities) as [HotelAmenity, boolean][])
          .filter(([, v]) => v === true).map(([k]) => k)
        setSelectedAmenities(new Set(selected))
      }

      if (s.attributes) {
        const attrs = s.attributes
        setCuisineType(typeof attrs.cuisineType === 'string' ? attrs.cuisineType : '')
        setOpeningHours(typeof attrs.openingHours === 'string' ? attrs.openingHours : '')
        setDressCode(typeof attrs.dressCode === 'string' ? attrs.dressCode : '')
        setDurationDays(attrs.durationDays != null ? String(attrs.durationDays) : '')
        setIncludes(typeof attrs.includes === 'string' ? attrs.includes : '')
        setExcludes(typeof attrs.excludes === 'string' ? attrs.excludes : '')
        setVehicleType(typeof attrs.vehicleType === 'string' ? attrs.vehicleType : '')
        setServiceArea(typeof attrs.serviceArea === 'string' ? attrs.serviceArea : '')
      }
    }).catch((e) => setError(getApiErrorMessage(e))).finally(() => setLoading(false))
  }, [id, isNew])

  // ── Build amenities / attributes maps ────────────────────────────────────
  function buildAmenities(): Record<string, boolean> | undefined {
    if (!isHotelOrResort) return undefined
    return HOTEL_AMENITIES.reduce((acc, key) => {
      acc[key] = selectedAmenities.has(key)
      return acc
    }, {} as Record<string, boolean>)
  }

  function buildAttributes(): Record<string, unknown> | undefined {
    if (isRestaurant) {
      const obj: Record<string, unknown> = {}
      if (cuisineType) obj.cuisineType = cuisineType
      if (openingHours) obj.openingHours = openingHours
      if (dressCode) obj.dressCode = dressCode
      return Object.keys(obj).length ? obj : undefined
    }
    if (isTrip) {
      const obj: Record<string, unknown> = {}
      const days = int(durationDays)
      if (days != null) obj.durationDays = days
      if (includes) obj.includes = includes
      if (excludes) obj.excludes = excludes
      return Object.keys(obj).length ? obj : undefined
    }
    if (isTaxi) {
      const obj: Record<string, unknown> = {}
      if (vehicleType) obj.vehicleType = vehicleType
      if (serviceArea) obj.serviceArea = serviceArea
      return Object.keys(obj).length ? obj : undefined
    }
    return undefined
  }

  const blocker = useBlocker(isDirty && !saving)

  // ── Submit ────────────────────────────────────────────────────────────────
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setFieldErrors({})

    const parsed = portalListingSchema.safeParse({ name, description, basePrice, currency })
    if (!parsed.success) {
      const errs = parsed.error.flatten().fieldErrors
      setFieldErrors({
        name: errs.name?.[0],
        description: errs.description?.[0],
        basePrice: errs.basePrice?.[0],
        currency: errs.currency?.[0],
      })
      return
    }

    const price = num(basePrice)
    const lat = latitude ? num(latitude) : undefined
    const lng = longitude ? num(longitude) : undefined
    const amenitiesMap = buildAmenities()
    const attributesMap = buildAttributes()

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
          address: !isTaxi ? address.trim() || undefined : undefined,
          basePrice: price ?? 0,
          currency: currency.trim() || 'USD',
          maxGuests: int(maxGuests),
          totalRooms: isHotelOrResort ? int(totalRooms) : undefined,
          availableRooms: isHotelOrResort ? int(availableRooms) : undefined,
          starRating: isHotelOrResort ? int(starRating) : undefined,
          checkInTime: isHotelOrResort ? checkInTime || undefined : undefined,
          checkOutTime: isHotelOrResort ? checkOutTime || undefined : undefined,
          latitude: lat,
          longitude: lng,
          policies: !isTaxi ? policies.trim() || undefined : undefined,
          amenities: amenitiesMap,
          attributes: attributesMap,
        })
        const row = created.data as ServiceDto
        setIsDirty(false)
        navigate(`/portal/listings/${row.id}`, { replace: true })
      } else if (id) {
        await portalAPI.updateService(id, {
          name: name.trim(),
          description: description.trim() || undefined,
          city: city.trim() || undefined,
          country: country.trim() || undefined,
          address: !isTaxi ? address.trim() || undefined : undefined,
          basePrice: price ?? 0,
          status,
          maxGuests: int(maxGuests),
          totalRooms: isHotelOrResort ? int(totalRooms) : undefined,
          availableRooms: isHotelOrResort ? int(availableRooms) : undefined,
          starRating: isHotelOrResort ? int(starRating) : undefined,
          checkInTime: isHotelOrResort ? checkInTime || undefined : undefined,
          checkOutTime: isHotelOrResort ? checkOutTime || undefined : undefined,
          latitude: lat,
          longitude: lng,
          policies: !isTaxi ? policies.trim() || undefined : undefined,
          amenities: amenitiesMap,
          attributes: attributesMap,
        })
        setIsDirty(false)
        navigate('/portal/listings')
      }
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  function toggleAmenity(key: HotelAmenity) {
    setSelectedAmenities((prev) => {
      const next = new Set(prev)
      next.has(key) ? next.delete(key) : next.add(key)
      return next
    })
  }

  if (!id) return <p className="text-slate-600 dark:text-slate-300">{t('portalPages.invalidRoute')}</p>

  const inputCls = 'mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100'
  const errInputCls = 'mt-1 w-full rounded-lg border border-red-400 bg-white px-3 py-2 text-slate-900 dark:border-red-500 dark:bg-slate-900 dark:text-slate-100'

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
          <form onSubmit={handleSubmit} onChange={() => setIsDirty(true)} className="space-y-4">

            {/* ── Basic info ─────────────────────────────────────────── */}
            <SectionHeader label={t('portalPages.sectionBasicInfo')} />

            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldName')}</label>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                className={fieldErrors.name ? errInputCls : inputCls}
              />
              {fieldErrors.name && <p className="mt-1 text-xs text-red-600 dark:text-red-400">{fieldErrors.name}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldDescription')}</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={3}
                className={fieldErrors.description ? errInputCls : inputCls}
              />
              {fieldErrors.description && <p className="mt-1 text-xs text-red-600 dark:text-red-400">{fieldErrors.description}</p>}
            </div>

            {/* ── Location ───────────────────────────────────────────── */}
            <SectionHeader label={t('portalPages.sectionLocation')} />

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldCity')}</label>
                <input value={city} onChange={(e) => setCity(e.target.value)} className={inputCls} />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldCountry')}</label>
                <input value={country} onChange={(e) => setCountry(e.target.value)} className={inputCls} />
              </div>
            </div>

            {!isTaxi && (
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldAddress')}</label>
                <input value={address} onChange={(e) => setAddress(e.target.value)} className={inputCls} />
              </div>
            )}

            {!isTaxi && (
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
                    {t('portalPages.fieldLatitude')}
                    <span className="ml-1 font-normal text-slate-400 dark:text-slate-500">({t('portalPages.fieldLatLngHint')})</span>
                  </label>
                  <input
                    type="number"
                    step="any"
                    value={latitude}
                    onChange={(e) => setLatitude(e.target.value)}
                    className={inputCls}
                    placeholder="e.g. 24.7136"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldLongitude')}</label>
                  <input
                    type="number"
                    step="any"
                    value={longitude}
                    onChange={(e) => setLongitude(e.target.value)}
                    className={inputCls}
                    placeholder="e.g. 46.6753"
                  />
                </div>
              </div>
            )}

            {/* ── Pricing ────────────────────────────────────────────── */}
            <SectionHeader label={t('portalPages.sectionPricing')} />

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldBasePrice')}</label>
                <input
                  value={basePrice}
                  onChange={(e) => setBasePrice(e.target.value)}
                  inputMode="decimal"
                  className={fieldErrors.basePrice ? errInputCls : inputCls}
                />
                {fieldErrors.basePrice && <p className="mt-1 text-xs text-red-600 dark:text-red-400">{fieldErrors.basePrice}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldCurrency')}</label>
                <input
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value)}
                  disabled={!isNew}
                  title={isNew ? undefined : t('portalPages.currencyLockedHint')}
                  className={`${fieldErrors.currency ? errInputCls : inputCls} disabled:opacity-60`}
                />
                {fieldErrors.currency && <p className="mt-1 text-xs text-red-600 dark:text-red-400">{fieldErrors.currency}</p>}
              </div>
            </div>

            {/* ── Type-specific details ──────────────────────────────── */}
            <SectionHeader label={t('portalPages.sectionTypeDetails')} />

            {/* Capacity row (all types) */}
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{maxGuestsLabel}</label>
              <input
                value={maxGuests}
                onChange={(e) => setMaxGuests(e.target.value)}
                inputMode="numeric"
                className="mt-1 max-w-xs rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
              />
            </div>

            {/* HOTEL / RESORT */}
            {isHotelOrResort && (
              <>
                <div className="grid gap-4 sm:grid-cols-3">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldStarRating')}</label>
                    <input
                      value={starRating}
                      onChange={(e) => setStarRating(e.target.value)}
                      inputMode="numeric"
                      className={inputCls}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldTotalRooms')}</label>
                    <input
                      value={totalRooms}
                      onChange={(e) => setTotalRooms(e.target.value)}
                      inputMode="numeric"
                      className={inputCls}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldAvailableRooms')}</label>
                    <input
                      value={availableRooms}
                      onChange={(e) => setAvailableRooms(e.target.value)}
                      inputMode="numeric"
                      className={inputCls}
                    />
                  </div>
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldCheckInTime')}</label>
                    <input
                      type="time"
                      value={checkInTime}
                      onChange={(e) => setCheckInTime(e.target.value)}
                      className={inputCls}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldCheckOutTime')}</label>
                    <input
                      type="time"
                      value={checkOutTime}
                      onChange={(e) => setCheckOutTime(e.target.value)}
                      className={inputCls}
                    />
                  </div>
                </div>

                <div>
                  <label className="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldAmenities')}</label>
                  <div className="flex flex-wrap gap-2">
                    {HOTEL_AMENITIES.map((key) => {
                      const checked = selectedAmenities.has(key)
                      return (
                        <button
                          key={key}
                          type="button"
                          onClick={() => toggleAmenity(key)}
                          className={`rounded-full border px-3 py-1 text-sm transition-colors ${
                            checked
                              ? 'border-primary bg-primary/10 text-primary dark:border-primary dark:bg-primary/20 dark:text-blue-300'
                              : 'border-slate-300 bg-white text-slate-600 hover:border-slate-400 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-300'
                          }`}
                        >
                          {t(`portalPages.amenity_${key}`)}
                        </button>
                      )
                    })}
                  </div>
                </div>
              </>
            )}

            {/* RESTAURANT */}
            {isRestaurant && (
              <>
                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldCuisineType')}</label>
                    <input
                      value={cuisineType}
                      onChange={(e) => setCuisineType(e.target.value)}
                      className={inputCls}
                      placeholder={t('portalPages.cuisineTypePlaceholder')}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldOpeningHours')}</label>
                    <input
                      value={openingHours}
                      onChange={(e) => setOpeningHours(e.target.value)}
                      className={inputCls}
                      placeholder="e.g. 08:00–22:00"
                    />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldDressCode')}</label>
                  <input
                    value={dressCode}
                    onChange={(e) => setDressCode(e.target.value)}
                    className={inputCls}
                    placeholder={t('portalPages.dressCodePlaceholder')}
                  />
                </div>
              </>
            )}

            {/* TRIP */}
            {isTrip && (
              <>
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldDurationDays')}</label>
                  <input
                    value={durationDays}
                    onChange={(e) => setDurationDays(e.target.value)}
                    inputMode="numeric"
                    className="mt-1 max-w-xs rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldIncludes')}</label>
                  <textarea
                    value={includes}
                    onChange={(e) => setIncludes(e.target.value)}
                    rows={3}
                    className={inputCls}
                    placeholder={t('portalPages.includesPlaceholder')}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldExcludes')}</label>
                  <textarea
                    value={excludes}
                    onChange={(e) => setExcludes(e.target.value)}
                    rows={3}
                    className={inputCls}
                    placeholder={t('portalPages.excludesPlaceholder')}
                  />
                </div>
              </>
            )}

            {/* TAXI */}
            {isTaxi && (
              <>
                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldVehicleType')}</label>
                    <select
                      value={vehicleType}
                      onChange={(e) => setVehicleType(e.target.value)}
                      className={inputCls}
                    >
                      <option value="">—</option>
                      {VEHICLE_TYPES.map((v) => (
                        <option key={v} value={v}>{t(`portalPages.vehicleType_${v}`)}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldServiceArea')}</label>
                    <input
                      value={serviceArea}
                      onChange={(e) => setServiceArea(e.target.value)}
                      className={inputCls}
                      placeholder={t('portalPages.serviceAreaPlaceholder')}
                    />
                  </div>
                </div>
              </>
            )}

            {/* ── Policies ───────────────────────────────────────────── */}
            {!isTaxi && (
              <>
                <SectionHeader label={t('portalPages.sectionPolicies')} />
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldPolicies')}</label>
                  <textarea
                    value={policies}
                    onChange={(e) => setPolicies(e.target.value)}
                    rows={4}
                    className={inputCls}
                    placeholder={t('portalPages.policiesPlaceholder')}
                  />
                </div>
              </>
            )}

            {/* ── Status (edit only) ─────────────────────────────────── */}
            {!isNew && (
              <>
                <SectionHeader label={t('portalPages.fieldStatus')} />
                <div>
                  <select
                    value={status}
                    onChange={(e) => setStatus(e.target.value as ServiceStatusDto)}
                    className={inputCls}
                  >
                    {SERVICE_STATUSES.map((x) => (
                      <option key={x} value={x}>{x}</option>
                    ))}
                  </select>
                </div>
              </>
            )}

            {/* ── Actions ────────────────────────────────────────────── */}
            <div className="flex flex-wrap gap-3 pt-2">
              <button type="submit" disabled={saving} className="dashboard-btn-primary disabled:opacity-50">
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

      <ConfirmDialog
        open={blocker.state === 'blocked'}
        onClose={() => blocker.reset?.()}
        title={t('portalPages.unsavedChangesTitle')}
        description={t('portalPages.unsavedChangesBody')}
        confirmLabel={t('portalPages.unsavedChangesLeave')}
        variant="danger"
        onConfirm={() => blocker.proceed?.()}
      />
    </>
  )
}
