/**
 * Create service provider (partner) — Super Admin path activates immediately; Sales path stays pending.
 */

import { useEffect, useState } from 'react'
import { getApiErrorMessage, providersAPI } from '../services/api'
import {
  PARTNER_SERVICE_TYPE_VALUES,
  type CreateServiceProviderPayload,
  type ServiceTypeDto,
} from '../types/api'
import { useLanguage } from '../context/LanguageContext'

export type CreateProviderVariant = 'management' | 'sales'

export function CreateProviderModal({
  open,
  onClose,
  variant,
  onCreated,
  presetServiceType,
}: {
  open: boolean
  onClose: () => void
  variant: CreateProviderVariant
  onCreated?: () => void
  /** When set, payload uses this enum and the type field is not shown (e.g. opened from a service vertical page). */
  presetServiceType?: ServiceTypeDto
}) {
  const { t } = useLanguage()
  const [linkMode, setLinkMode] = useState<'new' | 'existing'>('new')
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [address, setAddress] = useState('')
  const [email, setEmail] = useState('')
  const [type, setType] = useState<ServiceTypeDto | ''>('')
  const [registrationNumber, setRegistrationNumber] = useState('')
  const [description, setDescription] = useState('')
  const [managerEmail, setManagerEmail] = useState('')
  const [managerPassword, setManagerPassword] = useState('')
  const [managerPhone, setManagerPhone] = useState('')
  const [existingManagerEmail, setExistingManagerEmail] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    if (presetServiceType) {
      setType(presetServiceType)
    }
  }, [open, presetServiceType])

  if (!open) return null

  const reset = () => {
    setName('')
    setPhone('')
    setAddress('')
    setEmail('')
    setType(presetServiceType ?? '')
    setRegistrationNumber('')
    setDescription('')
    setManagerEmail('')
    setManagerPassword('')
    setManagerPhone('')
    setExistingManagerEmail('')
    setError(null)
    setSuccess(null)
    setLinkMode('new')
  }

  const handleClose = () => {
    reset()
    onClose()
  }

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
      setSuccess(
        variant === 'sales'
          ? t('createProviderModal.successSales')
          : t('createProviderModal.successActive'),
      )
      onCreated?.()
      setTimeout(() => {
        handleClose()
      }, 1200)
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      onClick={handleClose}
    >
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-700 dark:bg-slate-800"
        onClick={(ev) => ev.stopPropagation()}
      >
        <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">
          {t('createProviderModal.title')}
        </h3>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          {variant === 'sales' ? t('createProviderModal.introSales') : t('createProviderModal.introSuper')}
        </p>

        {error && (
          <div className="mt-3 rounded-lg border border-red-200 bg-red-50 p-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
            {error}
          </div>
        )}
        {success && (
          <div className="mt-3 rounded-lg border border-emerald-200 bg-emerald-50 p-2 text-sm text-emerald-800 dark:border-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-200">
            {success}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-4 space-y-3">
          <div className="flex gap-3 text-sm">
            <label className="flex cursor-pointer items-center gap-2">
              <input
                type="radio"
                name="linkMode"
                checked={linkMode === 'new'}
                onChange={() => setLinkMode('new')}
              />
              {t('createProviderModal.modeNew')}
            </label>
            <label className="flex cursor-pointer items-center gap-2">
              <input
                type="radio"
                name="linkMode"
                checked={linkMode === 'existing'}
                onChange={() => setLinkMode('existing')}
              />
              {t('createProviderModal.modeExisting')}
            </label>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">
              {t('createProviderModal.labelName')}
            </label>
            <input
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">
              {t('createProviderModal.labelPhone')}
            </label>
            <input
              required
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">
              {t('createProviderModal.labelAddress')}
            </label>
            <input
              required
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">
              {t('createProviderModal.labelContactEmail')}
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
          </div>
          {variant === 'management' && !presetServiceType && (
            <div>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">
                {t('createProviderModal.labelType')}
              </label>
              <select
                required
                value={type}
                onChange={(e) => setType((e.target.value || '') as ServiceTypeDto | '')}
                className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              >
                <option value="">{t('createProviderModal.typePlaceholder')}</option>
                {PARTNER_SERVICE_TYPE_VALUES.map((v) => (
                  <option key={v} value={v}>
                    {t(`serviceType.${v}`)}
                  </option>
                ))}
              </select>
            </div>
          )}
          <div>
            <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">
              {t('createProviderModal.labelReg')}
            </label>
            <input
              value={registrationNumber}
              onChange={(e) => setRegistrationNumber(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">
              {t('createProviderModal.labelDescription')}
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={2}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
          </div>

          {linkMode === 'new' ? (
            <>
              <div>
                <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">
                  {t('createProviderModal.labelManagerEmail')}
                </label>
                <input
                  type="email"
                  required
                  value={managerEmail}
                  onChange={(e) => setManagerEmail(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">
                  {t('createProviderModal.labelManagerPassword')}
                </label>
                <input
                  type="password"
                  required
                  minLength={6}
                  autoComplete="new-password"
                  value={managerPassword}
                  onChange={(e) => setManagerPassword(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">
                  {t('createProviderModal.labelManagerPhone')}
                </label>
                <input
                  value={managerPhone}
                  onChange={(e) => setManagerPhone(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                />
              </div>
            </>
          ) : (
            <div>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">
                {t('createProviderModal.labelExistingManagerEmail')}
              </label>
              <input
                required
                type="email"
                value={existingManagerEmail}
                onChange={(e) => setExistingManagerEmail(e.target.value)}
                placeholder="manager@example.com"
                className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              />
            </div>
          )}

          <div className="flex flex-wrap justify-end gap-2 pt-2">
            <button type="button" onClick={handleClose} className="dashboard-btn-secondary">
              {t('ui.cancel')}
            </button>
            <button type="submit" disabled={submitting} className="dashboard-btn-primary disabled:opacity-50">
              {submitting ? t('ui.loading') : t('createProviderModal.submit')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
