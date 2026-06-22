/**
 * Provider portal: GET/PUT /providers/me
 */

import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { usePermission } from '../../hooks/usePermission'
import { getApiErrorMessage, providersAPI } from '../../services/api'
import type { ServiceProviderDto } from '../../types/api'
import { Card } from '../../components/Card'

const inputCls = 'dashboard-date-input w-full'

function InfoRow({ label, value }: { label: string; value?: string | null }) {
  if (value == null) return null
  return (
    <div className="flex items-baseline gap-2 text-sm">
      <span className="min-w-[8rem] shrink-0 font-medium text-slate-500 dark:text-slate-400">{label}</span>
      <span className="text-slate-900 dark:text-slate-100">{value}</span>
    </div>
  )
}

function verifiedBadge(verified?: boolean | null) {
  if (verified == null) return null
  return verified
    ? <span className="badge badge-success">Verified</span>
    : <span className="badge badge-warning">Unverified</span>
}

export function PortalProfilePage() {
  const { t } = useLanguage()
  const canManage = usePermission('portal:manage')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [profile, setProfile] = useState<ServiceProviderDto | null>(null)

  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [address, setAddress] = useState('')

  useEffect(() => {
    setLoading(true)
    setError(null)
    providersAPI
      .getMe()
      .then((res) => {
        const p = res.data as ServiceProviderDto
        setProfile(p)
        setName(p.name ?? '')
        setPhone(p.phone ?? '')
        setEmail(p.email ?? '')
        setAddress(p.address ?? '')
      })
      .catch((e) => {
        setProfile(null)
        setError(getApiErrorMessage(e))
      })
      .finally(() => setLoading(false))
  }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    setSuccess(false)
    try {
      const updated = await providersAPI.updateMe({
        name: name.trim(),
        phone: phone.trim() || undefined,
        email: email.trim() || undefined,
        address: address.trim() || undefined,
      })
      setProfile(updated.data as ServiceProviderDto)
      setSuccess(true)
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <h1 className="app-page-title">{t('title.profile')}</h1>

      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
          {error}
        </div>
      )}

      {success && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:border-emerald-800/60 dark:bg-emerald-900/20 dark:text-emerald-300">
          {t('portalPages.saveProfile')} — changes saved.
        </div>
      )}

      {loading ? (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="h-12 animate-pulse rounded-xl bg-slate-100 dark:bg-white/[0.04]" />
          ))}
        </div>
      ) : !profile ? null : (
        <div className="grid gap-6 lg:grid-cols-[1fr_2fr]">
          {/* Info panel */}
          <Card className="!p-5 h-fit">
            <h2 className="dashboard-card-title mb-4">{t('portalPages.profileStatus')}</h2>
            <div className="flex flex-col gap-3">
              <InfoRow label={t('portalPages.profileStatus')} value={profile.status} />
              {profile.verified != null && (
                <div className="flex items-center gap-2 text-sm">
                  <span className="min-w-[8rem] shrink-0 font-medium text-slate-500 dark:text-slate-400">
                    {t('portalPages.profileVerified')}
                  </span>
                  {verifiedBadge(profile.verified)}
                </div>
              )}
              {profile.profitMargin != null && (
                <InfoRow
                  label={t('portalPages.profileCommission')}
                  value={`${String(profile.profitMargin)}%`}
                />
              )}
              {profile.type && (
                <InfoRow label={t('portalPages.profileType')} value={profile.type} />
              )}
            </div>
          </Card>

          {/* Edit form */}
          <Card className="!p-5">
            <h2 className="dashboard-card-title mb-5">{t('portalPages.saveProfile')}</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
                  {t('portalPages.fieldLegalName')}
                </label>
                <input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  className={inputCls}
                />
              </div>
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
                  {t('portalPages.fieldPhone')}
                </label>
                <input
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className={inputCls}
                />
              </div>
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
                  {t('portalPages.fieldEmail')}
                </label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className={inputCls}
                />
              </div>
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
                  {t('portalPages.fieldAddress')}
                </label>
                <input
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  className={inputCls}
                />
              </div>
              {canManage && (
                <div className="pt-1">
                  <button type="submit" disabled={saving} className="dashboard-btn-primary disabled:opacity-50">
                    {saving ? t('portalPages.saving') : t('portalPages.saveProfile')}
                  </button>
                </div>
              )}
            </form>
          </Card>
        </div>
      )}
    </>
  )
}
