/**
 * Provider portal: submit images (logo + service images) for admin approval.
 */

import { useCallback, useEffect, useRef, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { usePermission } from '../../hooks/usePermission'
import { getApiErrorMessage, portalAPI, portalMediaAPI } from '../../services/api'
import type { ProviderMediaSubmissionDto, ServiceDto } from '../../types/api'
import { Card } from '../../components/Card'

type Sub = ProviderMediaSubmissionDto & { _loading?: boolean }

function statusBadge(status: string, t: (k: string) => string) {
  if (status === 'APPROVED') return <span className="badge badge-success">{t('portalPages.mediaStatusApproved')}</span>
  if (status === 'REJECTED') return <span className="badge badge-danger">{t('portalPages.mediaStatusRejected')}</span>
  return <span className="badge badge-warning">{t('portalPages.mediaStatusPending')}</span>
}

function asServices(data: unknown): ServiceDto[] {
  if (data && typeof data === 'object' && Array.isArray((data as { content?: unknown[] }).content)) {
    return (data as { content: ServiceDto[] }).content
  }
  if (Array.isArray(data)) return data as ServiceDto[]
  return []
}

function asSubs(data: unknown): ProviderMediaSubmissionDto[] {
  if (Array.isArray(data)) return data as ProviderMediaSubmissionDto[]
  return []
}

export function PortalMediaPage() {
  const { t } = useLanguage()
  const canManage = usePermission('portal:manage')
  const logoFileRef = useRef<HTMLInputElement>(null)
  const serviceFileRef = useRef<HTMLInputElement>(null)

  const [submissions, setSubmissions] = useState<Sub[]>([])
  const [services, setServices] = useState<ServiceDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [logoAlt, setLogoAlt] = useState('')
  const [logoUploading, setLogoUploading] = useState(false)
  const [logoMsg, setLogoMsg] = useState<string | null>(null)

  const [selectedService, setSelectedService] = useState('')
  const [imgAlt, setImgAlt] = useState('')
  const [imgType, setImgType] = useState('SERVICE')
  const [imgUploading, setImgUploading] = useState(false)
  const [imgMsg, setImgMsg] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    Promise.all([
      portalMediaAPI.getMySubmissions(),
      portalAPI.listServices({ size: 100 }),
    ])
      .then(([subRes, svcRes]) => {
        setSubmissions(asSubs(subRes.data))
        setServices(asServices(svcRes.data))
      })
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  const handleLogoSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const file = logoFileRef.current?.files?.[0]
    if (!file) return
    setLogoUploading(true)
    setLogoMsg(null)
    try {
      const form = new FormData()
      form.append('file', file)
      if (logoAlt.trim()) form.append('altText', logoAlt.trim())
      await portalMediaAPI.submitProviderLogo(form)
      setLogoMsg(t('portalPages.mediaUploadSuccess'))
      if (logoFileRef.current) logoFileRef.current.value = ''
      setLogoAlt('')
      load()
    } catch (err) {
      setLogoMsg(getApiErrorMessage(err))
    } finally {
      setLogoUploading(false)
    }
  }

  const handleServiceImageSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedService) return
    const file = serviceFileRef.current?.files?.[0]
    if (!file) return
    setImgUploading(true)
    setImgMsg(null)
    try {
      const form = new FormData()
      form.append('file', file)
      if (imgAlt.trim()) form.append('altText', imgAlt.trim())
      form.append('imageType', imgType)
      await portalMediaAPI.submitServiceImage(selectedService, form)
      setImgMsg(t('portalPages.mediaUploadSuccess'))
      if (serviceFileRef.current) serviceFileRef.current.value = ''
      setImgAlt('')
      load()
    } catch (err) {
      setImgMsg(getApiErrorMessage(err))
    } finally {
      setImgUploading(false)
    }
  }

  return (
    <>
      <h1 className="app-page-title">{t('portalPages.mediaTitle')}</h1>
      <p className="text-sm text-slate-500 dark:text-slate-400">{t('portalPages.mediaIntro')}</p>

      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Logo upload */}
        <Card className="!p-5">
          <h2 className="dashboard-card-title mb-4">{t('portalPages.mediaLogo')}</h2>
          <form onSubmit={handleLogoSubmit} className="space-y-3">
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
                {t('portalPages.mediaAltText')}
              </label>
              <input
                value={logoAlt}
                onChange={(e) => setLogoAlt(e.target.value)}
                placeholder="e.g. Grand Palace Hotel logo"
                className="dashboard-date-input w-full"
              />
            </div>
            <div>
              <input
                ref={logoFileRef}
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                required
                className="block w-full text-sm text-slate-600 file:mr-3 file:rounded-lg file:border-0 file:bg-primary/10 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-primary hover:file:bg-primary/20 dark:text-slate-400"
              />
            </div>
            {logoMsg && (
              <p className="text-sm text-emerald-700 dark:text-emerald-400">{logoMsg}</p>
            )}
            {canManage && (
              <button
                type="submit"
                disabled={logoUploading}
                className="dashboard-btn-primary disabled:opacity-50"
              >
                {logoUploading ? t('portalPages.mediaUploading') : t('portalPages.mediaSubmitLogo')}
              </button>
            )}
          </form>
        </Card>

        {/* Service image upload */}
        <Card className="!p-5">
          <h2 className="dashboard-card-title mb-4">{t('portalPages.mediaServiceImages')}</h2>
          <form onSubmit={handleServiceImageSubmit} className="space-y-3">
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
                {t('portalPages.mediaSelectService')}
              </label>
              <select
                value={selectedService}
                onChange={(e) => setSelectedService(e.target.value)}
                required
                className="modal-select w-full"
              >
                <option value="">—</option>
                {services.map((s) => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
                {t('portalPages.mediaImageType')}
              </label>
              <select
                value={imgType}
                onChange={(e) => setImgType(e.target.value)}
                className="modal-select w-full"
              >
                <option value="SERVICE">Gallery</option>
                <option value="ROOM">Room</option>
                <option value="MENU_ITEM">Menu item</option>
                <option value="SWEETS_DESSERTS">Sweets &amp; Desserts</option>
                <option value="EXTERIOR">Exterior / Facade</option>
                <option value="POOL_FACILITIES">Pool &amp; Facilities</option>
                <option value="LOBBY">Lobby / Reception</option>
              </select>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
                {t('portalPages.mediaAltText')}
              </label>
              <input
                value={imgAlt}
                onChange={(e) => setImgAlt(e.target.value)}
                placeholder="e.g. Deluxe suite view"
                className="dashboard-date-input w-full"
              />
            </div>
            <div>
              <input
                ref={serviceFileRef}
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                required
                className="block w-full text-sm text-slate-600 file:mr-3 file:rounded-lg file:border-0 file:bg-primary/10 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-primary hover:file:bg-primary/20 dark:text-slate-400"
              />
            </div>
            {imgMsg && (
              <p className="text-sm text-emerald-700 dark:text-emerald-400">{imgMsg}</p>
            )}
            {canManage && (
              <button
                type="submit"
                disabled={imgUploading || !selectedService}
                className="dashboard-btn-primary disabled:opacity-50"
              >
                {imgUploading ? t('portalPages.mediaUploading') : t('portalPages.mediaSubmitImage')}
              </button>
            )}
          </form>
        </Card>
      </div>

      {/* Submissions table */}
      <Card className="!p-0 overflow-hidden">
        <div className="px-5 py-4">
          <h2 className="dashboard-card-title">{t('portalPages.mediaSubmissions')}</h2>
        </div>
        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : submissions.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-500 dark:text-slate-400">{t('portalPages.mediaNoSubmissions')}</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 dark:border-white/[0.05]">
                <th className="px-4 py-3 text-left font-medium text-slate-500 dark:text-slate-400">{t('portalPages.mediaColType')}</th>
                <th className="px-4 py-3 text-left font-medium text-slate-500 dark:text-slate-400">{t('portalPages.mediaColStatus')}</th>
                <th className="px-4 py-3 text-left font-medium text-slate-500 dark:text-slate-400">{t('portalPages.mediaColDate')}</th>
                <th className="px-4 py-3 text-left font-medium text-slate-500 dark:text-slate-400">Preview</th>
                <th className="px-4 py-3 text-left font-medium text-slate-500 dark:text-slate-400">{t('portalPages.mediaColNote')}</th>
              </tr>
            </thead>
            <tbody>
              {submissions.map((s) => (
                <tr key={s.id} className="border-b border-slate-50 last:border-0 dark:border-white/[0.03]">
                  <td className="px-4 py-3 text-slate-700 dark:text-slate-200 capitalize">{s.imageType?.toLowerCase()}</td>
                  <td className="px-4 py-3">{statusBadge(s.status, t)}</td>
                  <td className="px-4 py-3 text-slate-500 dark:text-slate-400">{s.submittedAt ? new Date(s.submittedAt).toLocaleDateString() : '—'}</td>
                  <td className="px-4 py-3">
                    {s.fileUrl ? (
                      <img
                        src={s.fileUrl}
                        alt={s.altText ?? ''}
                        className="h-12 w-16 rounded-lg object-cover border border-slate-200 dark:border-white/[0.06]"
                      />
                    ) : '—'}
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-500 dark:text-slate-400">{s.reviewNote ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </>
  )
}
