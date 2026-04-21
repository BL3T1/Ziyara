/**
 * Company staff: edit partner (provider) profile — description, branding URLs, contact details;
 * optional commission, verification, and status for permitted roles.
 */

import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, providersAPI } from '../../services/api'
import {
  PARTNER_SERVICE_TYPE_VALUES,
  type ProviderStatusDto,
  type ServiceProviderDto,
  type UpdateServiceProviderPayload,
} from '../../types/api'
import {
  canApproveRejectProvider,
  canViewProviderCommission,
  isCompanyStaffRole,
} from '../../types/auth'
import { isUuid } from '../../utils/isUuid'
import { safeImageUrl } from '../../utils/safeRendering'

const STATUS_OPTIONS: ProviderStatusDto[] = [
  'PENDING_APPROVAL',
  'PENDING_VERIFICATION',
  'ACTIVE',
  'SUSPENDED',
  'INACTIVE',
  'REJECTED',
  'BLOCKED',
]

export function EditProviderPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const navigate = useNavigate()
  const { providerId } = useParams<{ providerId: string }>()
  const id = providerId?.trim() ?? ''

  const showCommission = user?.role ? canViewProviderCommission(user.role) : false
  const showGovernance = user?.role ? canApproveRejectProvider(user.role) : false
  const allowed = user?.role ? isCompanyStaffRole(user.role) : false

  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [address, setAddress] = useState('')
  const [description, setDescription] = useState('')
  const [logoUrl, setLogoUrl] = useState('')
  const [commissionRate, setCommissionRate] = useState('')
  const [verified, setVerified] = useState(false)
  const [status, setStatus] = useState<ProviderStatusDto>('ACTIVE')

  const [readOnly, setReadOnly] = useState<
    Partial<Pick<ServiceProviderDto, 'type' | 'registrationNumber' | 'rating' | 'createdAt'>>
  >({})

  useEffect(() => {
    if (!id || !isUuid(id)) return
    setLoading(true)
    setError(null)
    providersAPI
      .get(id)
      .then((res) => {
        const p = res.data as ServiceProviderDto
        if (!p?.id) {
          setError(t('providerEditPage.notFound'))
          return
        }
        setName(p.name ?? '')
        setPhone(p.phone ?? '')
        setEmail(p.email ?? '')
        setAddress(p.address ?? '')
        setDescription(p.description ?? '')
        setLogoUrl(p.logoUrl ?? '')
        setCommissionRate(p.commissionRate != null ? String(p.commissionRate) : '')
        setVerified(!!p.verified)
        const st = (p.status ?? 'ACTIVE').toUpperCase() as ProviderStatusDto
        setStatus(STATUS_OPTIONS.includes(st) ? st : 'ACTIVE')
        setReadOnly({
          type: p.type,
          registrationNumber: p.registrationNumber,
          rating: p.rating,
          createdAt: p.createdAt,
        })
      })
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [id, t])

  if (!allowed) {
    return <Navigate to="/management/providers" replace />
  }

  if (!id || !isUuid(id)) {
    return <Navigate to="/management/providers" replace />
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    setSaving(true)
    try {
      const payload: UpdateServiceProviderPayload = {
        name: name.trim(),
        phone: phone.trim(),
        email: email.trim(),
        address: address.trim(),
        description: description.trim(),
        logoUrl: logoUrl.trim(),
      }
      if (showCommission) {
        const rate = parseFloat(commissionRate)
        if (commissionRate !== '' && (Number.isNaN(rate) || rate < 0 || rate > 100)) {
          setError(t('providerEditPage.errCommission'))
          setSaving(false)
          return
        }
        if (commissionRate !== '') {
          payload.commissionRate = rate
        }
      }
      if (showGovernance) {
        payload.verified = verified
        payload.status = status
      }
      const res = await providersAPI.update(id, payload)
      const p = res.data as ServiceProviderDto
      if (p?.name) setName(p.name)
      if (p?.phone != null) setPhone(p.phone)
      if (p?.email != null) setEmail(p.email)
      if (p?.address != null) setAddress(p.address)
      setDescription(p?.description ?? '')
      setLogoUrl(p?.logoUrl ?? '')
      if (p?.commissionRate != null) setCommissionRate(String(p.commissionRate))
      setVerified(!!p.verified)
      const st = (p?.status ?? status).toUpperCase() as ProviderStatusDto
      if (STATUS_OPTIONS.includes(st)) setStatus(st)
      setSuccess(t('providerEditPage.saved'))
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const logoPreview = safeImageUrl(logoUrl)

  return (
    <div className="mx-auto max-w-2xl">
      <Link to="/management/providers" className="text-sm font-medium text-primary hover:underline">
        ← {t('providerEditPage.backToList')}
      </Link>
      <h1 className="mt-4 text-2xl font-bold text-slate-900 dark:text-slate-100">{t('providerEditPage.title')}</h1>
      <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">{t('providerEditPage.intro')}</p>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}
      {success && (
        <div className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800 dark:border-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-200">
          {success}
        </div>
      )}

      <div className="mt-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
        {loading ? (
          <div className="py-12 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <dl className="grid gap-3 rounded-lg border border-slate-100 bg-slate-50/80 p-4 text-sm dark:border-slate-600 dark:bg-slate-900/40 sm:grid-cols-2">
              <div>
                <dt className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('providersPage.colType')}</dt>
                <dd className="mt-0.5 text-slate-900 dark:text-slate-100">
                  {readOnly.type && (PARTNER_SERVICE_TYPE_VALUES as readonly string[]).includes(readOnly.type)
                    ? t(`serviceType.${readOnly.type}` as 'serviceType.HOTEL')
                    : (readOnly.type ?? t('ui.emDash'))}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('createProviderModal.labelReg')}</dt>
                <dd className="mt-0.5 text-slate-900 dark:text-slate-100">{readOnly.registrationNumber ?? t('ui.emDash')}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('providersPage.ratingLabel')}</dt>
                <dd className="mt-0.5 text-slate-900 dark:text-slate-100">
                  {readOnly.rating != null ? String(readOnly.rating) : t('ui.emDash')}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('providersPage.colCreated')}</dt>
                <dd className="mt-0.5 text-slate-900 dark:text-slate-100">{readOnly.createdAt ?? t('ui.emDash')}</dd>
              </div>
            </dl>

            <div>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">{t('createProviderModal.labelName')}</label>
              <input
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">{t('createProviderModal.labelPhone')}</label>
              <input
                required
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">{t('createProviderModal.labelContactEmail')}</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">{t('createProviderModal.labelAddress')}</label>
              <input
                required
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">{t('createProviderModal.labelDescription')}</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={5}
                className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">{t('createProviderModal.labelLogoUrl')}</label>
              <input
                value={logoUrl}
                onChange={(e) => setLogoUrl(e.target.value)}
                placeholder="https://… or /media/…"
                className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              />
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{t('providerEditPage.hintListingImages')}</p>
              {logoPreview ? (
                <div className="mt-3">
                  <p className="text-xs font-medium text-slate-600 dark:text-slate-400">{t('providerEditPage.logoPreview')}</p>
                  <img
                    src={logoPreview}
                    alt=""
                    className="mt-2 h-20 w-auto max-w-full rounded-lg border border-slate-200 object-contain dark:border-slate-600"
                  />
                </div>
              ) : null}
            </div>

            {showCommission && (
              <div>
                <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">{t('providersPage.colCommission')} (%)</label>
                <input
                  type="number"
                  min="0"
                  max="100"
                  step="0.5"
                  value={commissionRate}
                  onChange={(e) => setCommissionRate(e.target.value)}
                  className="mt-1 w-full max-w-xs rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                />
              </div>
            )}

            {showGovernance && (
              <>
                <div>
                  <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">{t('providersPage.colStatus')}</label>
                  <select
                    value={status}
                    onChange={(e) => setStatus(e.target.value as ProviderStatusDto)}
                    className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                  >
                    {STATUS_OPTIONS.map((s) => (
                      <option key={s} value={s}>
                        {t(`providerEditPage.status.${s}`)}
                      </option>
                    ))}
                  </select>
                </div>
                <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-800 dark:text-slate-200">
                  <input type="checkbox" checked={verified} onChange={(e) => setVerified(e.target.checked)} />
                  {t('providerEditPage.verifiedLabel')}
                </label>
              </>
            )}

            <div className="flex flex-wrap justify-end gap-2 border-t border-slate-100 pt-4 dark:border-slate-700">
              <button type="button" onClick={() => navigate('/management/providers')} className="dashboard-btn-secondary">
                {t('ui.cancel')}
              </button>
              <button type="submit" disabled={saving} className="dashboard-btn-primary disabled:opacity-50">
                {saving ? t('ui.loading') : t('providersPage.save')}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
