/**
 * Provider portal: GET/PUT /providers/me
 */

import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, providersAPI } from '../../services/api'
import type { ServiceProviderDto } from '../../types/api'
import { Card } from '../../components/Card'

export function PortalProfilePage() {
  const { t } = useLanguage()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
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
    try {
      const updated = await providersAPI.updateMe({
        name: name.trim(),
        phone: phone.trim() || undefined,
        email: email.trim() || undefined,
        address: address.trim() || undefined,
      })
      setProfile(updated.data as ServiceProviderDto)
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
        <p className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
          {error}
        </p>
      )}

      {loading ? (
        <p className="mt-6 text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
      ) : !profile ? null : (
        <Card className="mt-6 p-6">
          <div className="mb-6 grid gap-2 text-sm text-slate-600 dark:text-slate-300 sm:grid-cols-2">
            <p>
              <span className="font-medium text-slate-800 dark:text-slate-200">{t('portalPages.profileStatus')}:</span>{' '}
              {profile.status}
            </p>
            {profile.verified != null && (
              <p>
                <span className="font-medium text-slate-800 dark:text-slate-200">{t('portalPages.profileVerified')}:</span>{' '}
                {profile.verified ? t('portalPages.yes') : t('portalPages.no')}
              </p>
            )}
            {profile.commissionRate != null && (
              <p>
                <span className="font-medium text-slate-800 dark:text-slate-200">{t('portalPages.profileCommission')}:</span>{' '}
                {String(profile.commissionRate)}%
              </p>
            )}
            {profile.type && (
              <p>
                <span className="font-medium text-slate-800 dark:text-slate-200">{t('portalPages.profileType')}:</span>{' '}
                {profile.type}
              </p>
            )}
          </div>

          <form onSubmit={handleSubmit} className="max-w-xl space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldLegalName')}</label>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldPhone')}</label>
              <input
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldEmail')}</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.fieldAddress')}</label>
              <input
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
              />
            </div>
            <button
              type="submit"
              disabled={saving}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {saving ? t('portalPages.saving') : t('portalPages.saveProfile')}
            </button>
          </form>
        </Card>
      )}
    </>
  )
}
