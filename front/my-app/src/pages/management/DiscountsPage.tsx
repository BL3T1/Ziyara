/**
 * Management > Discounts – list, filter, create, approve, deactivate, delete.
 */

import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { useAuth } from '../../context/AuthContext'
import { discountsAPI, providersAPI, servicesAPI } from '../../services/api'
import { getApiErrorMessage } from '../../services/api'
import type { DiscountDto, PageDto, ServiceDto, ServiceProviderDto } from '../../types/api'
import { canApproveDiscount, canCreateDiscount, isSuperAdminRole } from '../../types/auth'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'

const STATUS_FILTERS = [
  { id: 'ACTIVE', labelKey: 'discountsPage.statusActive' },
  { id: 'PENDING_APPROVAL', labelKey: 'discountsPage.statusPending' },
  { id: 'INACTIVE', labelKey: 'discountsPage.statusInactive' },
] as const

const TYPE_OPTIONS = [
  { id: 'PERCENTAGE', labelKey: 'discountsPage.typePercentage' },
  { id: 'FIXED_AMOUNT', labelKey: 'discountsPage.typeFixedAmount' },
] as const

const SPONSOR_OPTIONS = [
  { id: 'COMPANY', labelKey: 'discountsPage.sponsorCompany' },
  { id: 'PROVIDER', labelKey: 'discountsPage.sponsorProvider' },
  { id: 'BOTH', labelKey: 'discountsPage.sponsorBoth' },
] as const

function toLocalDateTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (Number.isNaN(d.getTime())) return dateStr
  return d.toISOString().slice(0, 16)
}

function parseUuidInput(raw: string): string[] | undefined {
  const ids = raw
    .split(/[\s,;\n]+/)
    .map((s) => s.trim())
    .filter(Boolean)
  return ids.length > 0 ? ids : undefined
}

function formatScopeSummary(d: DiscountDto, translate: (key: string) => string): string {
  const hasProvider = Boolean(d.providerId)
  const nList = d.applicableServiceIds?.length ?? 0
  const nMenu = (d.applicableMenuSectionIds?.length ?? 0) + (d.applicableMenuItemIds?.length ?? 0)
  const nRoom = d.applicableRoomTypeIds?.length ?? 0
  if (!hasProvider && nList === 0 && nMenu === 0 && nRoom === 0) {
    return translate('discountsPage.scopeAny')
  }
  const bits: string[] = []
  if (hasProvider) bits.push(translate('discountsPage.scopeProvider'))
  if (nList > 0) bits.push(translate('discountsPage.scopeListings').replace('{{count}}', String(nList)))
  if (nMenu > 0) bits.push(translate('discountsPage.scopeMenu'))
  if (nRoom > 0) bits.push(translate('discountsPage.scopeRooms'))
  return bits.join(' · ') || translate('discountsPage.scopeAny')
}

export function DiscountsPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const role = user?.role ?? 'user'
  const superAdmin = isSuperAdminRole(user?.role)
  const allowCreate = canCreateDiscount(role) || superAdmin
  const allowApprove = canApproveDiscount(role) || superAdmin
  const [discounts, setDiscounts] = useState<DiscountDto[]>([])
  const [filter, setFilter] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [createSubmitting, setCreateSubmitting] = useState(false)
  const [createForm, setCreateForm] = useState({
    code: '',
    description: '',
    type: 'PERCENTAGE',
    value: '',
    minBookingAmount: '',
    maxDiscountAmount: '',
    startDate: '' as string,
    endDate: '' as string,
    usageLimit: '',
    sponsor: 'COMPANY',
    providerId: '' as string,
    selectedListingIds: [] as string[],
    menuSectionUuids: '',
    menuItemUuids: '',
    roomTypeUuids: '',
  })
  const [providerOptions, setProviderOptions] = useState<ServiceProviderDto[]>([])
  const [listingOptions, setListingOptions] = useState<ServiceDto[]>([])

  const load = () => {
    setLoading(true)
    setError(null)
    const params = filter ? { status: filter, page: 0, size: 100 } : { page: 0, size: 100 }
    discountsAPI
      .list(params)
      .then((res) => {
        const data = res.data as PageDto<DiscountDto> | DiscountDto[]
        const list = Array.isArray(data) ? data : (data?.content ?? [])
        setDiscounts(list)
      })
      .catch((e) => {
        setError(getApiErrorMessage(e))
        setDiscounts([])
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [filter])

  useEffect(() => {
    if (!createModalOpen) return
    providersAPI
      .list({ page: 0, size: 200 })
      .then((res) => {
        const data = res.data as PageDto<ServiceProviderDto> | ServiceProviderDto[]
        const list = Array.isArray(data) ? data : (data?.content ?? [])
        setProviderOptions(list)
      })
      .catch(() => setProviderOptions([]))
  }, [createModalOpen])

  useEffect(() => {
    if (!createModalOpen || !createForm.providerId) {
      setListingOptions([])
      return
    }
    servicesAPI
      .list({ providerId: createForm.providerId, page: 0, size: 200 })
      .then((res) => {
        const data = res.data as PageDto<ServiceDto> | ServiceDto[]
        const list = Array.isArray(data) ? data : (data?.content ?? [])
        setListingOptions(list)
      })
      .catch(() => setListingOptions([]))
  }, [createModalOpen, createForm.providerId])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!createForm.code.trim() || !createForm.value || !createForm.endDate) return
    const valueNum = parseFloat(createForm.value)
    if (Number.isNaN(valueNum)) return
    setCreateSubmitting(true)
    try {
      await discountsAPI.create({
        code: createForm.code.trim().toUpperCase(),
        description: createForm.description.trim() || undefined,
        type: createForm.type,
        value: valueNum,
        minBookingAmount: createForm.minBookingAmount ? parseFloat(createForm.minBookingAmount) : undefined,
        maxDiscountAmount: createForm.maxDiscountAmount ? parseFloat(createForm.maxDiscountAmount) : undefined,
        startDate: createForm.startDate ? new Date(createForm.startDate).toISOString() : undefined,
        endDate: new Date(createForm.endDate).toISOString(),
        usageLimit: createForm.usageLimit ? parseInt(createForm.usageLimit, 10) : 0,
        sponsor: createForm.sponsor,
        providerId: createForm.providerId || undefined,
        applicableServiceIds:
          createForm.selectedListingIds.length > 0 ? createForm.selectedListingIds : undefined,
        applicableMenuSectionIds: parseUuidInput(createForm.menuSectionUuids),
        applicableMenuItemIds: parseUuidInput(createForm.menuItemUuids),
        applicableRoomTypeIds: parseUuidInput(createForm.roomTypeUuids),
      })
      setCreateModalOpen(false)
      setCreateForm({
        code: '',
        description: '',
        type: 'PERCENTAGE',
        value: '',
        minBookingAmount: '',
        maxDiscountAmount: '',
        startDate: '',
        endDate: '',
        usageLimit: '',
        sponsor: 'COMPANY',
        providerId: '',
        selectedListingIds: [],
        menuSectionUuids: '',
        menuItemUuids: '',
        roomTypeUuids: '',
      })
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setCreateSubmitting(false)
    }
  }

  const handleApprove = async (id: string) => {
    try {
      await discountsAPI.approve(id)
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  const handleDeactivate = async (id: string) => {
    try {
      await discountsAPI.deactivate(id)
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  const handleDelete = async (id: string) => {
    if (!window.confirm(t('discountsPage.confirmDelete'))) return
    try {
      await discountsAPI.delete(id)
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  const filtered =
    filter && discounts.length > 0
      ? discounts.filter((d) => (d.status ?? '').toUpperCase() === filter.toUpperCase())
      : discounts

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('discountsPage.title')}</h1>
        </div>
        {allowCreate && (
          <button
            type="button"
            onClick={() => setCreateModalOpen(true)}
            className="dashboard-btn-primary shrink-0"
          >
            {t('discountsPage.createDiscount')}
          </button>
        )}
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="mt-6 flex flex-wrap items-center gap-4">
        <button
          type="button"
          onClick={() => setFilter(null)}
          className={filter === null ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
        >
          {t('ui.all')}
        </button>
        {STATUS_FILTERS.map((card) => (
          <button
            key={card.id}
            type="button"
            onClick={() => setFilter(card.id)}
            className={filter === card.id ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
          >
            {t(card.labelKey)}
          </button>
        ))}
      </div>

      <div className="mt-6 table-shell">
        {loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('discountsPage.loading')}</div>
        ) : filtered.length === 0 ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('discountsPage.noDiscounts')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('discountsPage.colCode')}</th>
                <th className="px-4 py-3.5">{t('discountsPage.colType')}</th>
                <th className="px-4 py-3.5">{t('discountsPage.colSponsor')}</th>
                <th className="px-4 py-3.5">{t('discountsPage.colScope')}</th>
                <th className="px-4 py-3.5">{t('discountsPage.colValue')}</th>
                <th className="px-4 py-3.5">{t('discountsPage.colStatus')}</th>
                <th className="px-4 py-3.5">{t('discountsPage.colUsed')}</th>
                <th className="px-4 py-3.5">{t('discountsPage.colEndDate')}</th>
                <th className="px-4 py-3.5">{t('discountsPage.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((d) => (
                <tr key={d.id}>
                  <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-slate-900 dark:text-slate-100">{d.code}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{d.type ?? t('ui.emDash')}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {d.sponsor
                      ? t(
                          d.sponsor === 'PROVIDER'
                            ? 'discountsPage.sponsorProvider'
                            : d.sponsor === 'BOTH'
                              ? 'discountsPage.sponsorBoth'
                              : 'discountsPage.sponsorCompany',
                        )
                      : t('discountsPage.sponsorCompany')}
                  </td>
                  <td className="max-w-[14rem] px-4 py-3 text-xs text-slate-600 dark:text-slate-300">
                    {formatScopeSummary(d, t)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {d.type === 'PERCENTAGE' ? `${d.value}%` : d.value}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{d.status ?? t('ui.emDash')}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {d.usageCount ?? 0} {d.usageLimit ? `/ ${d.usageLimit}` : ''}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {d.endDate ? new Date(d.endDate).toLocaleDateString() : t('ui.emDash')}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    {d.status === 'PENDING_APPROVAL' && allowApprove && (
                      <button
                        type="button"
                        onClick={() => handleApprove(d.id)}
                        className="text-primary hover:underline"
                      >
                        {t('discountsPage.approve')}
                      </button>
                    )}
                    {d.status === 'ACTIVE' && (
                      <button
                        type="button"
                        onClick={() => handleDeactivate(d.id)}
                        className="text-amber-600 hover:underline dark:text-amber-400"
                      >
                        {t('discountsPage.deactivate')}
                      </button>
                    )}
                    <button
                      type="button"
                      onClick={() => handleDelete(d.id)}
                      className="ml-3 text-red-600 hover:underline dark:text-red-400"
                    >
                      {t('discountsPage.delete')}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <Modal
        open={createModalOpen}
        onClose={() => !createSubmitting && setCreateModalOpen(false)}
        title={t('discountsPage.modalTitle')}
        description={!allowApprove ? t('discountsPage.pendingHint') : undefined}
        footer={
          <>
            <button
              type="button"
              onClick={() => setCreateModalOpen(false)}
              disabled={createSubmitting}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="discount-create-form"
              disabled={createSubmitting}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {createSubmitting ? t('discountsPage.creating') : t('discountsPage.create')}
            </button>
          </>
        }
      >
        <form id="discount-create-form" onSubmit={handleCreate} className="space-y-5">
          {/* Two-column row for Type + Sponsor */}
          <div className="grid grid-cols-2 gap-4">
            <FormField label={t('discountsPage.labelCode')} required>
              <input
                type="text"
                value={createForm.code}
                onChange={(e) => setCreateForm((f) => ({ ...f, code: e.target.value.toUpperCase() }))}
                className="modal-input font-mono tracking-wider"
                placeholder="SUMMER20"
                required
              />
            </FormField>
            <FormField label={t('discountsPage.labelValue')} required>
              <input
                type="number"
                step="0.01"
                min="0"
                value={createForm.value}
                onChange={(e) => setCreateForm((f) => ({ ...f, value: e.target.value }))}
                className="modal-input"
                required
              />
            </FormField>
          </div>

          <FormField label={t('discountsPage.labelDescription')}>
            <input
              type="text"
              value={createForm.description}
              onChange={(e) => setCreateForm((f) => ({ ...f, description: e.target.value }))}
              className="modal-input"
            />
          </FormField>

          <div className="grid grid-cols-2 gap-4">
            <FormField label={t('discountsPage.labelType')}>
              <select
                value={createForm.type}
                onChange={(e) => setCreateForm((f) => ({ ...f, type: e.target.value }))}
                className="modal-select"
              >
                {TYPE_OPTIONS.map((o) => (
                  <option key={o.id} value={o.id}>
                    {t(o.labelKey)}
                  </option>
                ))}
              </select>
            </FormField>
            <FormField label={t('discountsPage.labelSponsor')}>
              <select
                value={createForm.sponsor}
                onChange={(e) => setCreateForm((f) => ({ ...f, sponsor: e.target.value }))}
                className="modal-select"
              >
                {SPONSOR_OPTIONS.map((o) => (
                  <option key={o.id} value={o.id}>
                    {t(o.labelKey)}
                  </option>
                ))}
              </select>
            </FormField>
          </div>

          <FormField label={t('discountsPage.labelProviderScope')}>
            <select
              value={createForm.providerId}
              onChange={(e) =>
                setCreateForm((f) => ({ ...f, providerId: e.target.value, selectedListingIds: [] }))
              }
              className="modal-select"
            >
              <option value="">{t('discountsPage.labelProviderAny')}</option>
              {providerOptions.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name ?? p.id}
                </option>
              ))}
            </select>
          </FormField>

          {createForm.providerId && (
            <FormField label={t('discountsPage.labelListings')} hint={t('discountsPage.listingsHint')}>
              <div className="max-h-36 space-y-1 overflow-y-auto rounded-xl border border-slate-200 p-2.5 dark:border-white/[0.08]">
                {listingOptions.length === 0 ? (
                  <p className="text-xs text-slate-500">{t('discountsPage.loading')}</p>
                ) : (
                  listingOptions.map((svc) => (
                    <label key={svc.id} className="flex cursor-pointer items-start gap-2 rounded-lg px-1.5 py-1 text-sm hover:bg-slate-50 dark:hover:bg-white/[0.04]">
                      <input
                        type="checkbox"
                        checked={createForm.selectedListingIds.includes(svc.id)}
                        onChange={(e) => {
                          const on = e.target.checked
                          setCreateForm((f) => ({
                            ...f,
                            selectedListingIds: on
                              ? [...f.selectedListingIds, svc.id]
                              : f.selectedListingIds.filter((id) => id !== svc.id),
                          }))
                        }}
                        className="mt-0.5"
                      />
                      <span className="text-slate-700 dark:text-slate-200">
                        {svc.name ?? svc.id}{' '}
                        <span className="text-slate-400">({String(svc.type ?? '')})</span>
                      </span>
                    </label>
                  ))
                )}
              </div>
            </FormField>
          )}

          {/* UUID scope fields — collapsible group */}
          <div className="space-y-4 rounded-xl border border-slate-100 p-4 dark:border-white/[0.05]">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              Optional UUID Scopes
            </p>
            <FormField label={t('discountsPage.labelMenuSections')} hint={t('discountsPage.uuidListHint')}>
              <textarea
                value={createForm.menuSectionUuids}
                onChange={(e) => setCreateForm((f) => ({ ...f, menuSectionUuids: e.target.value }))}
                rows={2}
                className="modal-textarea font-mono text-xs"
                placeholder="uuid-1, uuid-2"
              />
            </FormField>
            <FormField label={t('discountsPage.labelMenuItems')} hint={t('discountsPage.uuidListHint')}>
              <textarea
                value={createForm.menuItemUuids}
                onChange={(e) => setCreateForm((f) => ({ ...f, menuItemUuids: e.target.value }))}
                rows={2}
                className="modal-textarea font-mono text-xs"
              />
            </FormField>
            <FormField label={t('discountsPage.labelRoomTypes')} hint={t('discountsPage.uuidListHint')}>
              <textarea
                value={createForm.roomTypeUuids}
                onChange={(e) => setCreateForm((f) => ({ ...f, roomTypeUuids: e.target.value }))}
                rows={2}
                className="modal-textarea font-mono text-xs"
              />
            </FormField>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <FormField label={t('discountsPage.labelMinBooking')}>
              <input
                type="number"
                step="0.01"
                min="0"
                value={createForm.minBookingAmount}
                onChange={(e) => setCreateForm((f) => ({ ...f, minBookingAmount: e.target.value }))}
                className="modal-input"
              />
            </FormField>
            <FormField label={t('discountsPage.labelMaxDiscount')}>
              <input
                type="number"
                step="0.01"
                min="0"
                value={createForm.maxDiscountAmount}
                onChange={(e) => setCreateForm((f) => ({ ...f, maxDiscountAmount: e.target.value }))}
                className="modal-input"
              />
            </FormField>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <FormField label={t('discountsPage.labelStartDate')}>
              <input
                type="datetime-local"
                value={createForm.startDate ? toLocalDateTime(createForm.startDate) : ''}
                onChange={(e) =>
                  setCreateForm((f) => ({
                    ...f,
                    startDate: e.target.value ? new Date(e.target.value).toISOString() : '',
                  }))
                }
                className="modal-input"
              />
            </FormField>
            <FormField label={t('discountsPage.labelEndDate')} required>
              <input
                type="datetime-local"
                value={createForm.endDate ? toLocalDateTime(createForm.endDate) : ''}
                onChange={(e) =>
                  setCreateForm((f) => ({
                    ...f,
                    endDate: e.target.value ? new Date(e.target.value).toISOString() : '',
                  }))
                }
                className="modal-input"
                required
              />
            </FormField>
          </div>

          <FormField label={t('discountsPage.labelUsageLimit')}>
            <input
              type="number"
              min="0"
              value={createForm.usageLimit}
              onChange={(e) => setCreateForm((f) => ({ ...f, usageLimit: e.target.value }))}
              className="modal-input"
            />
          </FormField>
        </form>
      </Modal>
    </>
  )
}
