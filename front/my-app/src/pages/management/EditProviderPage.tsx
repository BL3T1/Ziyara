/**
 * Company staff: edit partner (provider) profile — description, branding URLs, contact details;
 * optional profit margin, verification, and status for permitted roles.
 */

import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { adminMediaAPI, getApiErrorMessage, providersAPI } from '../../services/api'
import { Modal } from '../../components/Modal'
import { PasswordInput } from '../../components/PasswordInput'
import {
  PARTNER_SERVICE_TYPE_VALUES,
  type ProviderMediaSubmissionDto,
  type ProviderStatusDto,
  type ServiceProviderDto,
  type UpdateServiceProviderPayload,
} from '../../types/api'
import { usePermission } from '../../hooks/usePermission'
import { isUuid } from '../../utils/isUuid'
import { safeImageUrl } from '../../utils/safeRendering'
import { FormField } from '../../components/FormField'

const STATUS_OPTIONS: ProviderStatusDto[] = [
  'PENDING_APPROVAL',
  'PENDING_VERIFICATION',
  'ACTIVE',
  'SUSPENDED',
  'INACTIVE',
  'REJECTED',
  'BLOCKED',
]

function SectionHeader({ label }: { label: string }) {
  return (
    <div className="border-b border-slate-100 pb-1 dark:border-white/[0.05]">
      <p className="text-xs font-semibold uppercase tracking-widest text-slate-400 dark:text-slate-500">{label}</p>
    </div>
  )
}

export function EditProviderPage() {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const { providerId } = useParams<{ providerId: string }>()
  const id = providerId?.trim() ?? ''

  const showProfitMargin = usePermission('payments:read')
  const showGovernance = usePermission('providers:approve')
  const showMediaApproval = usePermission('media_submissions:approve')
  const allowed = usePermission('providers:read')
  const canWrite = usePermission('providers:write')

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
  const [profitMargin, setProfitMargin] = useState('')
  const [globalRate, setGlobalRate] = useState('')
  const [verified, setVerified] = useState(false)
  const [status, setStatus] = useState<ProviderStatusDto>('ACTIVE')
  const [expiryDate, setExpiryDate] = useState('')
  const [resettingPassword, setResettingPassword] = useState(false)
  const [showResetModal, setShowResetModal] = useState(false)
  const [mediaSubs, setMediaSubs] = useState<ProviderMediaSubmissionDto[]>([])
  const [mediaLoading, setMediaLoading] = useState(false)
  const [mediaMsg, setMediaMsg] = useState<string | null>(null)
  const [rejectingSubId, setRejectingSubId] = useState<string | null>(null)
  const [rejectNote, setRejectNote] = useState('')
  const [processingSubId, setProcessingSubId] = useState<string | null>(null)

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
        if (!p?.id) { setError(t('providerEditPage.notFound')); return }
        setName(p.name ?? '')
        setPhone(p.phone ?? '')
        setEmail(p.email ?? '')
        setAddress(p.address ?? '')
        setDescription(p.description ?? '')
        setLogoUrl(p.logoUrl ?? '')
        setProfitMargin(p.profitMargin != null ? String(p.profitMargin) : '10')
        setGlobalRate(p.globalRate != null ? String(p.globalRate) : '')
        setVerified(!!p.verified)
        const st = (p.status ?? 'ACTIVE').toUpperCase() as ProviderStatusDto
        setStatus(STATUS_OPTIONS.includes(st) ? st : 'ACTIVE')
        setExpiryDate(p.expiryDate ?? '')
        setReadOnly({ type: p.type, registrationNumber: p.registrationNumber, rating: p.rating, createdAt: p.createdAt })
      })
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [id, t])

  const loadMedia = useCallback(() => {
    if (!id || !showMediaApproval) return
    setMediaLoading(true)
    adminMediaAPI
      .list()
      .then((res) => {
        const all = Array.isArray(res.data) ? (res.data as ProviderMediaSubmissionDto[]) : []
        setMediaSubs(all.filter((s) => s.providerId === id && s.status === 'PENDING'))
      })
      .catch(() => {})
      .finally(() => setMediaLoading(false))
  }, [id, showMediaApproval])

  useEffect(() => { loadMedia() }, [loadMedia])

  const handleMediaApprove = async (subId: string) => {
    setProcessingSubId(subId)
    setMediaMsg(null)
    try {
      await adminMediaAPI.approve(subId)
      setMediaMsg(t('providerEditPage.mediaApproveSuccess'))
      loadMedia()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setProcessingSubId(null)
    }
  }

  const handleMediaReject = async (subId: string) => {
    setProcessingSubId(subId)
    setMediaMsg(null)
    try {
      await adminMediaAPI.reject(subId, rejectNote.trim() || undefined)
      setMediaMsg(t('providerEditPage.mediaRejectSuccess'))
      setRejectingSubId(null)
      setRejectNote('')
      loadMedia()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setProcessingSubId(null)
    }
  }

  if (!allowed) return <Navigate to="/management/providers" replace />
  if (!id || !isUuid(id)) return <Navigate to="/management/providers" replace />

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
      const grVal = parseFloat(globalRate)
      if (!globalRate || Number.isNaN(grVal) || grVal < 1 || grVal > 5) {
        setError(t('providerEditPage.errGlobalRate'))
        setSaving(false)
        return
      }
      payload.globalRate = grVal
      if (showProfitMargin) {
        const rate = parseFloat(profitMargin)
        if (profitMargin !== '' && (Number.isNaN(rate) || rate < 0 || rate > 100)) {
          setError(t('providerEditPage.errCommission'))
          setSaving(false)
          return
        }
        if (profitMargin !== '') payload.profitMargin = rate
      }
      if (showGovernance) {
        payload.verified = verified
        payload.status = status
        if (expiryDate) {
          if (new Date(expiryDate) <= new Date()) {
            setError(t('providerEditPage.errExpiryDateFuture'))
            setSaving(false)
            return
          }
          payload.expiryDate = expiryDate
        }
      }
      const res = await providersAPI.update(id, payload)
      const p = res.data as ServiceProviderDto
      if (p?.name) setName(p.name)
      if (p?.phone != null) setPhone(p.phone)
      if (p?.email != null) setEmail(p.email)
      if (p?.address != null) setAddress(p.address)
      setDescription(p?.description ?? '')
      setLogoUrl(p?.logoUrl ?? '')
      if (p?.profitMargin != null) setProfitMargin(String(p.profitMargin))
      if (p?.globalRate != null) setGlobalRate(String(p.globalRate))
      setVerified(!!p.verified)
      const st = (p?.status ?? status).toUpperCase() as ProviderStatusDto
      if (STATUS_OPTIONS.includes(st)) setStatus(st)
      if (p?.expiryDate != null) setExpiryDate(p.expiryDate)
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
      <Link to="/management/providers" className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline">
        ← {t('providerEditPage.backToList')}
      </Link>

      <div className="mt-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">{t('providerEditPage.title')}</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t('providerEditPage.intro')}</p>
        </div>
      </div>

      {error && (
        <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}
      {success && (
        <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:border-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-200">
          {success}
        </div>
      )}

      <div className="mt-6 rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-white/[0.06] dark:bg-[#0d1117]">
        {loading ? (
          <div className="py-16 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : (
          <form onSubmit={handleSubmit} className="divide-y divide-slate-100 dark:divide-white/[0.05]">

            {/* Read-only meta */}
            <div className="p-6">
              <SectionHeader label={t('providerEditPage.sectionInfo')} />
              <dl className="mt-4 grid gap-3 rounded-xl border border-slate-100 bg-slate-50/60 p-4 text-sm dark:border-white/[0.04] dark:bg-white/[0.02] sm:grid-cols-2">
                <div>
                  <dt className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('providersPage.colType')}</dt>
                  <dd className="mt-0.5 font-medium text-slate-900 dark:text-slate-100">
                    {readOnly.type && (PARTNER_SERVICE_TYPE_VALUES as readonly string[]).includes(readOnly.type)
                      ? t(`serviceType.${readOnly.type}` as 'serviceType.HOTEL')
                      : (readOnly.type ?? t('ui.emDash'))}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('createProviderModal.labelReg')}</dt>
                  <dd className="mt-0.5 font-medium font-mono text-slate-900 dark:text-slate-100">{readOnly.registrationNumber ?? t('ui.emDash')}</dd>
                </div>
                <div>
                  <dt className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('providersPage.ratingLabel')}</dt>
                  <dd className="mt-0.5 font-medium text-slate-900 dark:text-slate-100">
                    {readOnly.rating != null ? `${readOnly.rating} / 5` : t('ui.emDash')}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('providersPage.colCreated')}</dt>
                  <dd className="mt-0.5 font-medium text-slate-900 dark:text-slate-100">{readOnly.createdAt ?? t('ui.emDash')}</dd>
                </div>
              </dl>
            </div>

            {/* Contact details */}
            <div className="space-y-4 p-6">
              <SectionHeader label={t('providerEditPage.sectionContact')} />
              <div className="grid gap-4 sm:grid-cols-2">
                <FormField label={t('createProviderModal.labelName')} required>
                  <input
                    required
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="modal-input"
                  />
                </FormField>
                <FormField label={t('createProviderModal.labelPhone')} required>
                  <input
                    required
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    className="modal-input"
                  />
                </FormField>
              </div>
              <FormField label={t('createProviderModal.labelContactEmail')}>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="modal-input"
                />
              </FormField>
              <FormField label={t('createProviderModal.labelAddress')} required>
                <input
                  required
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  className="modal-input"
                />
              </FormField>
              <FormField label={t('providerEditPage.labelGlobalRate')} hint={t('providerEditPage.hintGlobalRate')} required>
                <div className="relative max-w-xs">
                  <input
                    required
                    type="number"
                    min="1"
                    max="5"
                    step="0.5"
                    value={globalRate}
                    onChange={(e) => setGlobalRate(e.target.value)}
                    placeholder="e.g. 3.0"
                    className="modal-input pr-12 font-mono"
                  />
                  <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-sm font-medium text-slate-400">★</span>
                </div>
              </FormField>
            </div>

            {/* Branding */}
            <div className="space-y-4 p-6">
              <SectionHeader label={t('providerEditPage.sectionBranding')} />
              <FormField label={t('createProviderModal.labelDescription')}>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  rows={4}
                  className="modal-textarea"
                />
              </FormField>
              <FormField label={t('createProviderModal.labelLogoUrl')} hint={t('providerEditPage.hintListingImages')}>
                <input
                  value={logoUrl}
                  onChange={(e) => setLogoUrl(e.target.value)}
                  placeholder="https://… or /media/…"
                  className="modal-input"
                />
              </FormField>
              {logoPreview && (
                <div>
                  <p className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('providerEditPage.logoPreview')}</p>
                  <img
                    src={logoPreview}
                    alt=""
                    className="mt-2 h-20 w-auto max-w-full rounded-xl border border-slate-200 object-contain dark:border-white/[0.06]"
                  />
                </div>
              )}
            </div>

            {/* Profit margin */}
            {showProfitMargin && (
              <div className="space-y-4 p-6">
                <SectionHeader label={t('providerEditPage.sectionFinancial')} />
                <FormField
                  label={t('createProviderModal.labelProfitMargin')}
                  hint={t('createProviderModal.hintProfitMargin')}
                >
                  <div className="relative max-w-xs">
                    <input
                      type="number"
                      min="0"
                      max="100"
                      step="0.5"
                      value={profitMargin}
                      onChange={(e) => setProfitMargin(e.target.value)}
                      className="modal-input pr-8 font-mono"
                    />
                    <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-sm font-medium text-slate-400">%</span>
                  </div>
                </FormField>
              </div>
            )}

            {/* Password reset */}
            <div className="space-y-4 p-6">
              <SectionHeader label={t('providerEditPage.sectionSecurity')} />
              <div className="flex items-center gap-4">
                <button
                  type="button"
                  disabled={resettingPassword}
                  onClick={() => setShowResetModal(true)}
                  className="dashboard-btn-secondary disabled:opacity-50"
                >
                  {t('providerEditPage.resetPassword')}
                </button>
                <p className="text-xs text-slate-500 dark:text-slate-400">{t('providerEditPage.hintResetPassword')}</p>
              </div>
            </div>

            {/* Governance */}
            {showGovernance && (
              <div className="space-y-4 p-6">
                <SectionHeader label={t('providerEditPage.sectionGovernance')} />
                <div className="grid gap-4 sm:grid-cols-2">
                  <FormField label={t('providersPage.colStatus')}>
                    <select
                      value={status}
                      onChange={(e) => setStatus(e.target.value as ProviderStatusDto)}
                      className="modal-select"
                    >
                      {STATUS_OPTIONS.map((s) => (
                        <option key={s} value={s}>{t(`providerEditPage.status.${s}`)}</option>
                      ))}
                    </select>
                  </FormField>
                  <FormField label={t('providerEditPage.labelExpiryDate')} hint={t('providerEditPage.hintExpiryDate')}>
                    <div className="flex items-center gap-2">
                      <input
                        type="date"
                        value={expiryDate}
                        min={new Date(Date.now() + 86400000).toISOString().split('T')[0]}
                        onChange={(e) => setExpiryDate(e.target.value)}
                        className="modal-input"
                      />
                      {expiryDate && new Date(expiryDate) < new Date() && (
                        <span className="shrink-0 rounded-full bg-red-100 px-2 py-0.5 text-xs font-semibold text-red-700 dark:bg-red-900/30 dark:text-red-300">
                          {t('providersPage.expired')}
                        </span>
                      )}
                      {expiryDate && new Date(expiryDate) >= new Date() && (() => {
                        const daysLeft = Math.floor((new Date(expiryDate).getTime() - Date.now()) / 86400000)
                        return daysLeft <= 7 ? (
                          <span className="shrink-0 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-700 dark:bg-amber-900/30 dark:text-amber-300">
                            {t('providersPage.expiringSoon')} ({daysLeft}d)
                          </span>
                        ) : null
                      })()}
                    </div>
                  </FormField>
                </div>
                <label className="flex cursor-pointer items-center gap-2.5 text-sm font-medium text-slate-800 dark:text-slate-200">
                  <input
                    type="checkbox"
                    checked={verified}
                    onChange={(e) => setVerified(e.target.checked)}
                    className="h-4 w-4 rounded border-slate-300 text-primary"
                  />
                  {t('providerEditPage.verifiedLabel')}
                </label>
              </div>
            )}

            {/* Footer */}
            <div className="flex flex-wrap justify-end gap-3 p-6">
              <button type="button" onClick={() => navigate('/management/providers')} className="dashboard-btn-secondary">
                {t('ui.cancel')}
              </button>
              {canWrite && (
                <button type="submit" disabled={saving} className="dashboard-btn-primary disabled:opacity-50">
                  {saving ? t('ui.loading') : t('providersPage.save')}
                </button>
              )}
            </div>
          </form>
        )}
      </div>

      {showMediaApproval && (
        <div className="mt-6 rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-white/[0.06] dark:bg-[#0d1117]">
          <div className="p-6">
            <SectionHeader label={t('providerEditPage.sectionMediaSubmissions')} />
            {mediaMsg && (
              <div className="mt-3 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700 dark:border-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-300">
                {mediaMsg}
              </div>
            )}
            {mediaLoading ? (
              <div className="py-6 text-center text-sm text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
            ) : mediaSubs.length === 0 ? (
              <p className="mt-4 text-sm text-slate-500 dark:text-slate-400">{t('providerEditPage.mediaNoPending')}</p>
            ) : (
              <div className="mt-4 space-y-3">
                {mediaSubs.map((sub) => (
                  <div
                    key={sub.id}
                    className="flex flex-wrap items-start gap-4 rounded-xl border border-slate-100 bg-slate-50/60 p-4 dark:border-white/[0.04] dark:bg-white/[0.02]"
                  >
                    {sub.fileUrl && (
                      <a href={sub.fileUrl} target="_blank" rel="noopener noreferrer" className="shrink-0">
                        <img
                          src={sub.fileUrl}
                          alt={sub.altText ?? ''}
                          className="h-16 w-24 rounded-lg border border-slate-200 object-cover dark:border-white/[0.06] hover:opacity-80 transition-opacity"
                        />
                      </a>
                    )}
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium capitalize text-slate-700 dark:text-slate-200">
                        {sub.imageType?.toLowerCase().replace('_', ' ')}
                        {sub.primary && (
                          <span className="ml-2 rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700 dark:bg-blue-900/30 dark:text-blue-300">
                            Primary
                          </span>
                        )}
                      </p>
                      <p className="mt-0.5 text-xs text-slate-400 dark:text-slate-500">
                        {sub.submittedAt ? new Date(sub.submittedAt).toLocaleDateString() : '—'}
                      </p>
                      {rejectingSubId === sub.id ? (
                        <div className="mt-2 flex flex-col gap-2">
                          <input
                            value={rejectNote}
                            onChange={(e) => setRejectNote(e.target.value)}
                            placeholder={t('mediaSubmissionsPage.rejectNote')}
                            className="rounded border border-slate-300 px-2 py-1 text-xs dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                          />
                          <div className="flex gap-2">
                            <button
                              type="button"
                              disabled={processingSubId === sub.id}
                              onClick={() => handleMediaReject(sub.id)}
                              className="text-xs font-semibold text-red-600 hover:underline disabled:opacity-40 dark:text-red-400"
                            >
                              {t('mediaSubmissionsPage.reject')}
                            </button>
                            <button
                              type="button"
                              onClick={() => { setRejectingSubId(null); setRejectNote('') }}
                              className="text-xs text-slate-500 hover:underline"
                            >
                              {t('ui.cancel')}
                            </button>
                          </div>
                        </div>
                      ) : (
                        <div className="mt-2 flex gap-3">
                          <button
                            type="button"
                            disabled={processingSubId === sub.id}
                            onClick={() => handleMediaApprove(sub.id)}
                            className="text-sm font-semibold text-green-600 hover:underline disabled:opacity-40 dark:text-green-400"
                          >
                            {t('mediaSubmissionsPage.approve')}
                          </button>
                          <button
                            type="button"
                            onClick={() => setRejectingSubId(sub.id)}
                            className="text-sm font-semibold text-red-600 hover:underline dark:text-red-400"
                          >
                            {t('mediaSubmissionsPage.reject')}
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {showResetModal && (
        <ProviderResetPasswordModal
          onClose={() => setShowResetModal(false)}
          onConfirm={async (newPassword) => {
            setResettingPassword(true)
            setError(null)
            try {
              await providersAPI.resetPassword(id, { newPassword })
              setSuccess(t('providerEditPage.resetPasswordSuccess'))
              setShowResetModal(false)
            } catch (err) {
              setError(getApiErrorMessage(err))
            } finally {
              setResettingPassword(false)
            }
          }}
        />
      )}
    </div>
  )
}

function ProviderResetPasswordModal({
  onClose,
  onConfirm,
}: {
  onClose: () => void
  onConfirm: (pw: string) => Promise<void>
}) {
  const { t } = useLanguage()
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [localError, setLocalError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (password.length < 6) { setLocalError('Password must be at least 6 characters.'); return }
    if (password !== confirm) { setLocalError('Passwords do not match.'); return }
    setSubmitting(true)
    setLocalError('')
    try {
      await onConfirm(password)
    } catch {
      // error shown by parent
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={t('providerEditPage.resetPassword')}
      size="sm"
      footer={
        <>
          <button type="button" onClick={onClose} disabled={submitting} className="dashboard-btn-secondary">
            {t('ui.cancel')}
          </button>
          <button type="submit" form="edit-provider-reset-pw-form" disabled={submitting} className="dashboard-btn-primary disabled:opacity-70">
            {t('staffUserPage.resetPasswordSubmit')}
          </button>
        </>
      }
    >
      {localError && (
        <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
          {localError}
        </div>
      )}
      <form id="edit-provider-reset-pw-form" onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('staffUserPage.resetPasswordLabel')}
          </label>
          <PasswordInput
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="modal-input mt-1.5 w-full"
            autoFocus
            required
            minLength={6}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('portalStaffPage.confirmPasswordLabel')}
          </label>
          <PasswordInput
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            className="modal-input mt-1.5 w-full"
            required
            minLength={6}
          />
        </div>
      </form>
    </Modal>
  )
}
