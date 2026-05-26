/**
 * Shared fields for creating a service provider (management = active; sales = pending).
 */

import { useEffect, useState } from 'react'
import { getApiErrorMessage, providersAPI } from '../services/api'
import {
  PARTNER_SERVICE_TYPE_VALUES,
  type CreateServiceProviderPayload,
  type ServiceTypeDto,
} from '../types/api'
import { useLanguage } from '../context/LanguageContext'
import { FormField } from './FormField'

export type CreateProviderVariant = 'management' | 'sales'

export function CreateProviderForm({
  variant,
  presetServiceType,
  onCancel,
  onCreated,
  successCloseMs,
}: {
  variant: CreateProviderVariant
  presetServiceType?: ServiceTypeDto
  onCancel: () => void
  onCreated?: () => void
  successCloseMs?: number
}) {
  const { t } = useLanguage()
  const [linkMode, setLinkMode] = useState<'new' | 'existing'>('new')
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [address, setAddress] = useState('')
  const [email, setEmail] = useState('')
  const [type, setType] = useState<ServiceTypeDto | ''>(presetServiceType ?? '')
  const [registrationNumber, setRegistrationNumber] = useState('')
  const [description, setDescription] = useState('')
  const [logoUrl, setLogoUrl] = useState('')
  const [profitMargin, setProfitMargin] = useState('10')
  const [managerEmail, setManagerEmail] = useState('')
  const [managerPassword, setManagerPassword] = useState('')
  const [managerPhone, setManagerPhone] = useState('')
  const [existingManagerEmail, setExistingManagerEmail] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    if (presetServiceType) setType(presetServiceType)
  }, [presetServiceType])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    setSubmitting(true)
    try {
      const resolvedType = presetServiceType ?? (type || undefined)
      if (!resolvedType) {
        setError(t('createProviderModal.errTypeRequired'))
        setSubmitting(false)
        return
      }
      const payload: CreateServiceProviderPayload = {
        name: name.trim(),
        phone: phone.trim(),
        address: address.trim(),
        email: email.trim() || undefined,
        type: resolvedType,
        registrationNumber: registrationNumber.trim() || undefined,
        description: description.trim() || undefined,
        logoUrl: logoUrl.trim() || undefined,
      }
      if (variant === 'management') {
        const rate = parseFloat(profitMargin)
        payload.commissionRate = Number.isFinite(rate) && rate >= 0 && rate <= 100 ? rate : 10
      }
      if (linkMode === 'new') {
        payload.managerEmail = managerEmail.trim()
        payload.managerPassword = managerPassword
        if (managerPhone.trim()) payload.managerPhone = managerPhone.trim()
      } else {
        const managerEmailValue = existingManagerEmail.trim()
        if (!managerEmailValue) {
          setError(t('createProviderModal.errExistingManagerEmail'))
          setSubmitting(false)
          return
        }
        payload.managerEmail = managerEmailValue
      }
      await providersAPI.create(payload)
      setSuccess(variant === 'sales' ? t('createProviderModal.successSales') : t('createProviderModal.successActive'))
      onCreated?.()
      if (successCloseMs != null && successCloseMs >= 0) {
        window.setTimeout(() => onCancel(), successCloseMs)
      }
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  if (success) {
    return (
      <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800 dark:border-emerald-800/50 dark:bg-emerald-900/20 dark:text-emerald-200">
        {success}
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      {/* ── Section 1: Provider info ───────────────────── */}
      <div className="space-y-4">
        <div className="border-b border-slate-100 pb-1 dark:border-white/[0.05]">
          <p className="text-xs font-semibold uppercase tracking-widest text-slate-400 dark:text-slate-500">
            {t('createProviderModal.sectionProvider')}
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField label={t('createProviderModal.labelName')} required>
            <input
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Grand Palace Hotel"
              className="modal-input"
            />
          </FormField>

          {variant === 'management' && !presetServiceType && (
            <FormField label={t('createProviderModal.labelType')} required>
              <select
                required
                value={type}
                onChange={(e) => setType((e.target.value || '') as ServiceTypeDto | '')}
                className="modal-select"
              >
                <option value="">{t('createProviderModal.typePlaceholder')}</option>
                {PARTNER_SERVICE_TYPE_VALUES.map((v) => (
                  <option key={v} value={v}>{t(`serviceType.${v}`)}</option>
                ))}
              </select>
            </FormField>
          )}

          {presetServiceType && (
            <FormField label={t('createProviderModal.labelType')}>
              <input
                readOnly
                value={t(`serviceType.${presetServiceType}`)}
                className="modal-input bg-slate-50 text-slate-500 dark:bg-white/[0.02]"
              />
            </FormField>
          )}
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField label={t('createProviderModal.labelPhone')} required>
            <input
              required
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="+966 5x xxx xxxx"
              className="modal-input"
            />
          </FormField>
          <FormField label={t('createProviderModal.labelContactEmail')}>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="contact@provider.com"
              className="modal-input"
            />
          </FormField>
        </div>

        <FormField label={t('createProviderModal.labelAddress')} required>
          <input
            required
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            placeholder="Street, City, Country"
            className="modal-input"
          />
        </FormField>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField label={t('createProviderModal.labelReg')}>
            <input
              value={registrationNumber}
              onChange={(e) => setRegistrationNumber(e.target.value)}
              placeholder="CR-XXXXXXXX"
              className="modal-input font-mono tracking-wide"
            />
          </FormField>

          {variant === 'management' && (
            <FormField
              label={t('createProviderModal.labelProfitMargin')}
              hint={t('createProviderModal.hintProfitMargin')}
            >
              <div className="relative">
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
          )}
        </div>

        <FormField label={t('createProviderModal.labelDescription')}>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            placeholder={t('createProviderModal.placeholderDescription')}
            className="modal-textarea"
          />
        </FormField>

        <FormField label={t('createProviderModal.labelLogoUrl')} hint="https://… or /media/…">
          <input
            type="url"
            value={logoUrl}
            onChange={(e) => setLogoUrl(e.target.value)}
            placeholder="https://example.com/logo.png"
            className="modal-input"
          />
        </FormField>
      </div>

      {/* ── Section 2: Manager account ─────────────────── */}
      <div className="space-y-4">
        <div className="border-b border-slate-100 pb-1 dark:border-white/[0.05]">
          <p className="text-xs font-semibold uppercase tracking-widest text-slate-400 dark:text-slate-500">
            {t('createProviderModal.sectionManager')}
          </p>
        </div>

        <div className="flex gap-4 text-sm">
          <label className="flex cursor-pointer items-center gap-2 font-medium text-slate-700 dark:text-slate-200">
            <input
              type="radio"
              name="linkMode"
              checked={linkMode === 'new'}
              onChange={() => setLinkMode('new')}
              className="text-primary"
            />
            {t('createProviderModal.modeNew')}
          </label>
          <label className="flex cursor-pointer items-center gap-2 font-medium text-slate-700 dark:text-slate-200">
            <input
              type="radio"
              name="linkMode"
              checked={linkMode === 'existing'}
              onChange={() => setLinkMode('existing')}
              className="text-primary"
            />
            {t('createProviderModal.modeExisting')}
          </label>
        </div>

        {linkMode === 'new' ? (
          <>
            <FormField label={t('createProviderModal.labelManagerEmail')} required>
              <input
                type="email"
                required
                value={managerEmail}
                onChange={(e) => setManagerEmail(e.target.value)}
                placeholder="manager@provider.com"
                className="modal-input"
              />
            </FormField>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <FormField label={t('createProviderModal.labelManagerPassword')} required>
                <input
                  type="password"
                  required
                  minLength={6}
                  autoComplete="new-password"
                  value={managerPassword}
                  onChange={(e) => setManagerPassword(e.target.value)}
                  className="modal-input"
                />
              </FormField>
              <FormField label={t('createProviderModal.labelManagerPhone')}>
                <input
                  type="tel"
                  value={managerPhone}
                  onChange={(e) => setManagerPhone(e.target.value)}
                  placeholder="+966 5x xxx xxxx"
                  className="modal-input"
                />
              </FormField>
            </div>
          </>
        ) : (
          <FormField
            label={t('createProviderModal.labelExistingManagerEmail')}
            hint={t('createProviderModal.hintExistingManager')}
            required
          >
            <input
              required
              type="email"
              value={existingManagerEmail}
              onChange={(e) => setExistingManagerEmail(e.target.value)}
              placeholder="manager@example.com"
              className="modal-input"
            />
          </FormField>
        )}
      </div>

      <div className="flex flex-wrap justify-end gap-3 border-t border-slate-100 pt-4 dark:border-white/[0.05]">
        <button type="button" onClick={onCancel} className="dashboard-btn-secondary">
          {t('ui.cancel')}
        </button>
        <button
          type="submit"
          disabled={submitting}
          className="dashboard-btn-primary disabled:opacity-50"
        >
          {submitting ? t('ui.loading') : t('createProviderModal.submit')}
        </button>
      </div>
    </form>
  )
}
