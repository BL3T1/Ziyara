/**
 * Admin > Roles – list roles, create, edit permissions, delete with reassignment.
 */

import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { usePermissions } from '../../context/PermissionsContext'
import { rolesAPI, getApiErrorMessage } from '../../services/api'
import type { RoleDto, GroupDto, PermissionCatalogueItemDto } from '../../types/api'
import { ALL_SIDEBAR_ITEM_IDS, SIDEBAR_SECTIONS } from '../../config/sidebar'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'


const PROVIDER_ROLE_CODES = new Set(['PROVIDER_MANAGER', 'PROVIDER_FINANCE', 'PROVIDER_STAFF', 'TAXI_OPERATOR'])

function isProviderRole(role: RoleDto): boolean {
  if (!role.code) return false
  return PROVIDER_ROLE_CODES.has(role.code) || role.code.startsWith('PROVIDER_')
}

export function RolesPage() {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const { has } = usePermissions()
  const canWrite = has('roles:write')
  const [roles, setRoles] = useState<RoleDto[]>([])
  const [groups, setGroups] = useState<GroupDto[]>([])
  const [catalogue, setCatalogue] = useState<PermissionCatalogueItemDto[]>([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<'company' | 'provider'>('company')
  const [showCreate, setShowCreate] = useState(false)
  const [editDetailsRole, setEditDetailsRole] = useState<RoleDto | null>(null)
  const [deleteRole, setDeleteRole] = useState<RoleDto | null>(null)
  const [editNavRole, setEditNavRole] = useState<RoleDto | null>(null)
  const [error, setError] = useState('')

  const load = () => {
    setLoading(true)
    Promise.all([
      rolesAPI.list().then((r) => (r.data as RoleDto[]) ?? []),
      rolesAPI.getGroups().then((r) => (r.data as GroupDto[]) ?? []),
      rolesAPI.getPermissionCatalogue().then((r) => (r.data as PermissionCatalogueItemDto[]) ?? []),
    ])
      .then(([r, g, c]) => {
        setRoles(Array.isArray(r) ? r : [])
        setGroups(Array.isArray(g) ? g : [])
        setCatalogue(Array.isArray(c) ? c : [])
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [])

  const roleIdFromUrl = searchParams.get('roleId')?.trim()

  useEffect(() => {
    if (!roleIdFromUrl || roles.length === 0) return
    const match = roles.find((r) => r.id === roleIdFromUrl)
    if (match) {
      setEditNavRole(match)
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev)
        next.delete('roleId')
        return next
      }, { replace: true })
    }
  }, [roleIdFromUrl, roles, setSearchParams])

  if (loading) {
    return <div className="text-slate-500 dark:text-slate-400">{t('rolesPage.loading')}</div>
  }

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('rolesPage.title')}</h1>
        </div>
        {canWrite && (
          <button
            type="button"
            onClick={() => setShowCreate(true)}
            className="dashboard-btn-primary shrink-0"
          >
            {t('rolesPage.createRoleButton')}
          </button>
        )}
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="mt-6 flex gap-1 border-b border-slate-200 dark:border-slate-700">
        {(['company', 'provider'] as const).map((tab) => (
          <button
            key={tab}
            type="button"
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2.5 text-sm font-medium transition-colors ${
              activeTab === tab
                ? 'border-b-2 border-primary text-primary dark:text-secondary dark:border-secondary'
                : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200'
            }`}
          >
            {tab === 'company' ? t('rolesPage.tabCompany') : t('rolesPage.tabProvider')}
          </button>
        ))}
      </div>

      <div className="mt-4 table-shell">
        <table>
          <thead>
            <tr>
              <th className="px-4 py-3.5">{t('rolesPage.colName')}</th>
              <th className="px-4 py-3.5">{t('rolesPage.colCode')}</th>
              <th className="px-4 py-3.5">{t('rolesPage.colUsers')}</th>
              <th className="px-4 py-3.5 text-end">{t('rolesPage.colActions')}</th>
            </tr>
          </thead>
          <tbody>
            {roles.filter((r) => activeTab === 'provider' ? isProviderRole(r) : !isProviderRole(r)).map((role) => (
              <tr key={role.id}>
                <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-slate-900 dark:text-slate-100">{role.name}</td>
                <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{role.code ?? '—'}</td>
                <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                  {(role.userCount ?? 0) > 0 ? (
                    <button
                      type="button"
                      onClick={() => navigate(`/admin/roles/${role.id}/members`, { state: { roleName: role.name } })}
                      className="font-medium text-primary hover:underline dark:text-secondary"
                      title={t('rolesPage.viewUsersAria')}
                    >
                      {role.userCount}
                    </button>
                  ) : (
                    <span>0</span>
                  )}
                </td>
                <td className="whitespace-nowrap px-4 py-3 text-right text-sm">
                  {canWrite ? (
                    <>
                      <button
                        type="button"
                        onClick={() => setEditNavRole(role)}
                        className="text-primary hover:underline"
                      >
                        {t('rolesPage.editPermissions')}
                      </button>
                      <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                      <button
                        type="button"
                        onClick={() => setEditDetailsRole(role)}
                        className="text-primary hover:underline"
                      >
                        {t('rolesPage.editDetails')}
                      </button>
                      <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                      <button
                        type="button"
                        onClick={() => setDeleteRole(role)}
                        className="text-red-600 hover:underline dark:text-red-400"
                      >
                        {t('rolesPage.delete')}
                      </button>
                    </>
                  ) : (
                    <span className="text-slate-400 dark:text-slate-500">{t('ui.emDash')}</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="mt-4 text-sm text-slate-500 dark:text-slate-400">
        {t('rolesPage.footerCounts', { groups: groups.length, perms: catalogue.length })}
      </p>

      {showCreate && (
        <CreateRoleModal
          groups={groups}
          onClose={() => setShowCreate(false)}
          onSuccess={() => {
            setShowCreate(false)
            setError('')
            load()
          }}
          setError={setError}
        />
      )}

      {editDetailsRole && (
        <EditRoleDetailsModal
          role={editDetailsRole}
          groups={groups}
          onClose={() => setEditDetailsRole(null)}
          onSuccess={() => {
            setEditDetailsRole(null)
            setError('')
            load()
          }}
          setError={setError}
        />
      )}

      {deleteRole && (
        <DeleteRoleModal
          role={deleteRole}
          roles={roles.filter((r) => r.id !== deleteRole.id)}
          onClose={() => setDeleteRole(null)}
          onSuccess={() => {
            setDeleteRole(null)
            setError('')
            load()
          }}
          setError={setError}
        />
      )}

      {editNavRole && (
        <EditRoleNavigationModal
          role={editNavRole}
          catalogue={catalogue}
          onClose={() => setEditNavRole(null)}
          onSuccess={() => {
            setEditNavRole(null)
            setError('')
            load()
          }}
          setError={setError}
        />
      )}
    </>
  )
}

type NavPermTab = { label: string; read?: string; write?: string }
type NavPermEntry = { read?: string; write?: string; approve?: string; tabs?: NavPermTab[] }

/** Maps each sidebar item to the permission codes it controls (read + optional write + optional approve). */
const NAV_PERMISSION_MAP: Record<string, NavPermEntry> = {
  dashboard:          {},
  main_find_customer: { read: 'customers:read' },
  main_deleted_items: {
    tabs: [
      { label: 'Company',   read: 'deleted_items:company:read',   write: 'deleted_items:company:restore' },
      { label: 'Providers', read: 'deleted_items:providers:read', write: 'deleted_items:providers:restore' },
      { label: 'Users',     read: 'deleted_items:users:read',     write: 'deleted_items:users:restore' },
    ],
  },
  hotels:             { read: 'services:read',  write: 'services:write',            approve: 'services:publish' },
  resorts:            { read: 'services:read',  write: 'services:write' },
  restaurants:        { read: 'services:read',  write: 'services:write' },
  taxis:              { read: 'taxi:read',       write: 'taxi:write' },
  trips:              { read: 'services:read',  write: 'services:write' },
  providers:          { read: 'providers:read', write: 'providers:write',            approve: 'providers:approve' },
  bookings:           { read: 'bookings:read',  write: 'bookings:write' },
  payments:           { read: 'payments:read',  write: 'payments:write' },
  payouts:            { read: 'payouts:read',   write: 'payouts:write' },
  discounts:          { read: 'discounts:read', write: 'discounts:write',            approve: 'discounts:approve' },
  media_approvals:    { read: 'media_submissions:approve' },
  reports:            { read: 'reports:read' },
  taxi_trips:         { read: 'taxi:read',       write: 'taxi:write' },
  currency_rates:     { read: 'currency:read',  write: 'currency:write' },
  complaints:         { read: 'complaints:read', write: 'complaints:write' },
  reviews:            { read: 'reviews:read',   write: 'reviews:write',              approve: 'reviews:moderate' },
  provider_messages:  { read: 'providers_messages:read', write: 'providers_messages:write' },
  settings:           { read: 'settings:read',  write: 'settings:write' },
  users:              { read: 'users:read',      write: 'users:write' },
  roles:              { read: 'roles:read',      write: 'roles:write' },
  logs:               { read: 'audit:read' },
  content:            { read: 'content:read',   write: 'content:write' },
  api:                { read: 'settings:read' },
  integrations:       { read: 'settings:read' },
  webhooks:           { read: 'webhooks:read',  write: 'webhooks:write' },
  subscriptions:      { read: 'providers:read' },
}

/** All permission codes that appear in NAV_PERMISSION_MAP (used to separate nav-managed vs standalone perms). */
const NAV_MANAGED_PERMS = new Set(
  Object.values(NAV_PERMISSION_MAP).flatMap((m) => {
    const codes: string[] = []
    if (m.read) codes.push(m.read)
    if (m.write) codes.push(m.write)
    if (m.approve) codes.push(m.approve)
    m.tabs?.forEach((t) => { if (t.read) codes.push(t.read); if (t.write) codes.push(t.write) })
    return codes
  })
)

function EditRoleNavigationModal({
  role,
  catalogue,
  onClose,
  onSuccess,
  setError,
}: {
  role: RoleDto
  catalogue: PermissionCatalogueItemDto[]
  onClose: () => void
  onSuccess: () => void
  setError: (s: string) => void
}) {
  const { t } = useLanguage()
  const { refreshPermissions } = usePermissions()
  const [selected, setSelected] = useState<Set<string>>(new Set())
  /** permCode → checked: tracks which write/approve permissions are explicitly enabled (read is auto-granted with nav item) */
  const [permChecked, setPermChecked] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [localError, setLocalError] = useState('')

  const catalogueCodes = new Set(catalogue.map((p) => p.code))
  const unlockedCodes = new Set(catalogue.filter((p) => !p.locked).map((p) => p.code))

  useEffect(() => {
    rolesAPI
      .get(role.id)
      .then((r) => {
        const data = r.data as RoleDto & { permissionIds?: string[] }
        const ids = data?.navigationItemIds ?? []
        setSelected(ids.length ? new Set(ids) : new Set())

        const rawPermIds: string[] = data?.permissionIds ?? data?.permissions ?? []
        const permIdToCode = new Map(catalogue.map((p) => [p.id, p.code]))
        const existingCodes = rawPermIds.map((id) => permIdToCode.get(id) ?? id).filter(Boolean)
        setPermChecked(new Set(existingCodes.filter((c) => NAV_MANAGED_PERMS.has(c))))
      })
      .catch(() => { setSelected(new Set()); setPermChecked(new Set()) })
      .finally(() => setLoading(false))
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [role.id])

  const toggleTab = (id: string) => {
    setSelected((prev) => {
      const next = new Set(prev)
      const map = NAV_PERMISSION_MAP[id] ?? {}
      const tabPerms = (map.tabs ?? []).flatMap((t) => [t.read, t.write].filter(Boolean) as string[])
      if (next.has(id)) {
        next.delete(id)
        setPermChecked((pc) => {
          const npc = new Set(pc)
          if (map.read) npc.delete(map.read)
          if (map.write) npc.delete(map.write)
          if (map.approve) npc.delete(map.approve)
          tabPerms.forEach((c) => npc.delete(c))
          return npc
        })
      } else {
        next.add(id)
        const reads: string[] = []
        if (map.read) reads.push(map.read)
        ;(map.tabs ?? []).forEach((t) => { if (t.read) reads.push(t.read) })
        if (reads.length) setPermChecked((pc) => new Set([...pc, ...reads]))
      }
      return next
    })
  }

  const togglePerm = (code: string) => {
    setPermChecked((prev) => {
      const next = new Set(prev)
      if (next.has(code)) next.delete(code)
      else next.add(code)
      return next
    })
  }

  const orderedVisibleIds = () => ALL_SIDEBAR_ITEM_IDS.filter((id) => selected.has(id))

  const buildPermissionIds = () => {
    return catalogue.filter((p) => p.code && permChecked.has(p.code)).map((p) => p.id)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLocalError('')
    setError('')
    setSubmitting(true)
    try {
      await rolesAPI.updateNavigation(role.id, { visibleItemIds: orderedVisibleIds() })
      await rolesAPI.updatePermissions(role.id, { permissionIds: buildPermissionIds() })
      refreshPermissions()
      onSuccess()
    } catch (err) {
      const msg = getApiErrorMessage(err)
      setLocalError(msg)
      setError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={t('rolesPage.editPermTitle', { name: role.name })}
      size="lg"
      footer={
        !loading ? (
          <>
            <button type="button" onClick={onClose} disabled={submitting} className="dashboard-btn-secondary">
              {t('ui.cancel')}
            </button>
            <button type="submit" form="role-nav-form" disabled={submitting} className="dashboard-btn-primary disabled:opacity-70">
              {t('rolesPage.save')}
            </button>
          </>
        ) : undefined
      }
    >
      {localError && (
        <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
          {localError}
        </div>
      )}
      {loading ? (
        <div className="py-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
      ) : (
        <form id="role-nav-form" onSubmit={handleSubmit} className="space-y-4">
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => {
                const newSelected = new Set<string>()
                const allPerms = new Set<string>()
                ALL_SIDEBAR_ITEM_IDS.forEach((id) => {
                  const m: NavPermEntry = NAV_PERMISSION_MAP[id] ?? {}
                  // Only include this nav item if its read permission is unlocked (or it has no permission gate)
                  const readIsUnlocked = !m.read || unlockedCodes.has(m.read)
                  if (!readIsUnlocked) return
                  newSelected.add(id)
                  if (m.read && unlockedCodes.has(m.read)) allPerms.add(m.read)
                  if (m.write && unlockedCodes.has(m.write)) allPerms.add(m.write)
                  if (m.approve && unlockedCodes.has(m.approve)) allPerms.add(m.approve)
                  m.tabs?.forEach((tab) => {
                    if (tab.read && unlockedCodes.has(tab.read)) allPerms.add(tab.read)
                    if (tab.write && unlockedCodes.has(tab.write)) allPerms.add(tab.write)
                  })
                })
                setSelected(newSelected)
                setPermChecked(allPerms)
              }}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 dark:border-slate-600 dark:text-slate-300"
            >
              {t('rolesPage.navPresetFull')}
            </button>
            <button
              type="button"
              onClick={() => { setSelected(new Set()); setPermChecked(new Set()) }}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 dark:border-slate-600 dark:text-slate-300"
            >
              {t('rolesPage.navPresetClear')}
            </button>
          </div>
          <div className="space-y-4 rounded-xl border border-slate-100 p-3.5 dark:border-white/[0.05]">
            {SIDEBAR_SECTIONS.map((section) => (
              <div key={section.id}>
                <div className="mb-2 text-xs font-bold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  {t(`section.${section.id}`)}
                </div>
                <div className="space-y-2">
                  {section.items.map((item) => {
                    const map: NavPermEntry = NAV_PERMISSION_MAP[item.id] ?? {}
                    const isVisible = selected.has(item.id)
                    const hasWrite = !!map.write && catalogueCodes.has(map.write)
                    const hasTabs = !!map.tabs?.length
                    return (
                      <div key={item.id}>
                        <label className="flex cursor-pointer items-center gap-2 py-0.5">
                          <input
                            type="checkbox"
                            checked={isVisible}
                            onChange={() => toggleTab(item.id)}
                            className="rounded border-slate-300 text-primary"
                          />
                          <span className="text-sm font-medium text-slate-700 dark:text-slate-200">
                            {t(`nav.${item.id}`)}
                          </span>
                        </label>
                        {isVisible && hasTabs && (
                          <div className="ms-6 mt-1 space-y-1">
                            {map.tabs!.map((tab) => (
                              <div key={tab.label} className="flex items-center gap-4">
                                <span className="w-20 text-xs font-medium text-slate-600 dark:text-slate-400">{tab.label}</span>
                                {tab.write && catalogueCodes.has(tab.write) && (
                                  <label className="flex cursor-pointer items-center gap-1.5">
                                    <input
                                      type="checkbox"
                                      checked={permChecked.has(tab.write)}
                                      onChange={() => togglePerm(tab.write!)}
                                      className="rounded border-slate-300 text-primary"
                                    />
                                    <span className="text-xs text-slate-500 dark:text-slate-400">Restore</span>
                                  </label>
                                )}
                              </div>
                            ))}
                          </div>
                        )}
                        {isVisible && !hasTabs && (hasWrite || (!!map.approve && catalogueCodes.has(map.approve))) && (
                          <div className="ms-6 mt-0.5 flex gap-4">
                            {hasWrite && (
                              <label className="flex cursor-pointer items-center gap-1.5">
                                <input
                                  type="checkbox"
                                  checked={permChecked.has(map.write!)}
                                  onChange={() => togglePerm(map.write!)}
                                  className="rounded border-slate-300 text-primary"
                                />
                                <span className="text-xs text-slate-500 dark:text-slate-400">Write</span>
                              </label>
                            )}
                            {map.approve && catalogueCodes.has(map.approve) && (
                              <label className="flex cursor-pointer items-center gap-1.5">
                                <input
                                  type="checkbox"
                                  checked={permChecked.has(map.approve)}
                                  onChange={() => togglePerm(map.approve!)}
                                  className="rounded border-slate-300 text-primary"
                                />
                                <span className="text-xs text-slate-500 dark:text-slate-400">Approve</span>
                              </label>
                            )}
                          </div>
                        )}
                      </div>
                    )
                  })}
                </div>
              </div>
            ))}
          </div>

        </form>
      )}
    </Modal>
  )
}

function EditRoleDetailsModal({
  role,
  groups,
  onClose,
  onSuccess,
  setError,
}: {
  role: RoleDto
  groups: GroupDto[]
  onClose: () => void
  onSuccess: () => void
  setError: (s: string) => void
}) {
  const { t } = useLanguage()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [nameAr, setNameAr] = useState('')
  const [descriptionAr, setDescriptionAr] = useState('')
  const [groupId, setGroupId] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [localError, setLocalError] = useState('')

  useEffect(() => {
    rolesAPI
      .get(role.id)
      .then((r) => {
        const d = r.data as RoleDto
        setName((d?.name ?? role.name ?? '').trim())
        setDescription((d?.description ?? '').trim())
        setNameAr('')
        setDescriptionAr('')
        setGroupId(d?.groupId ?? role.groupId ?? '')
      })
      .catch(() => {
        setName(role.name ?? '')
        setDescription('')
        setNameAr('')
        setDescriptionAr('')
        setGroupId(role.groupId ?? '')
      })
      .finally(() => setLoading(false))
  }, [role.id, role.name, role.groupId])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLocalError('')
    setError('')
    if (!name.trim()) {
      setLocalError(t('rolesPage.errNameRequired'))
      return
    }
    setSubmitting(true)
    try {
      await rolesAPI.updateDetails(role.id, {
        name: name.trim(),
        description: description.trim() || undefined,
        nameAr: nameAr.trim() || undefined,
        descriptionAr: descriptionAr.trim() || undefined,
        groupId: groupId || null,
      })
      onSuccess()
    } catch (err) {
      const msg = getApiErrorMessage(err)
      setLocalError(msg)
      setError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={t('rolesPage.editDetailsTitle', { name: role.name })}
      description={t('rolesPage.editDetailsHint')}
      size="md"
      footer={
        !loading ? (
          <>
            <button type="button" onClick={onClose} disabled={submitting} className="dashboard-btn-secondary">
              {t('ui.cancel')}
            </button>
            <button type="submit" form="role-details-form" disabled={submitting} className="dashboard-btn-primary disabled:opacity-70">
              {t('rolesPage.save')}
            </button>
          </>
        ) : undefined
      }
    >
      {localError && (
        <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
          {localError}
        </div>
      )}
      {loading ? (
        <div className="py-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
      ) : (
        <form id="role-details-form" onSubmit={handleSubmit} className="space-y-4">
          <FormField label={t('rolesPage.nameLabel')} required>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="modal-input"
              required
            />
          </FormField>
          <FormField label={t('rolesPage.descriptionLabel')}>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="modal-input"
            />
          </FormField>
          <FormField label={t('rolesPage.groupLabel')}>
            <select
              value={groupId}
              onChange={(e) => setGroupId(e.target.value)}
              className="modal-select"
            >
              <option value="">{t('rolesPage.groupNone')}</option>
              {groups.map((g) => (
                <option key={g.id} value={g.id}>{g.name}</option>
              ))}
            </select>
          </FormField>
          <div className="grid grid-cols-2 gap-4">
            <FormField label={t('rolesPage.nameArLabel')}>
              <input
                type="text"
                value={nameAr}
                onChange={(e) => setNameAr(e.target.value)}
                className="modal-input"
                dir="rtl"
              />
            </FormField>
            <FormField label={t('rolesPage.descriptionArLabel')}>
              <input
                type="text"
                value={descriptionAr}
                onChange={(e) => setDescriptionAr(e.target.value)}
                className="modal-input"
                dir="rtl"
              />
            </FormField>
          </div>
        </form>
      )}
    </Modal>
  )
}

function CreateRoleModal({
  groups,
  onClose,
  onSuccess,
  setError,
}: {
  groups: GroupDto[]
  onClose: () => void
  onSuccess: () => void
  setError: (s: string) => void
}) {
  const { t } = useLanguage()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [groupId, setGroupId] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [localError, setLocalError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLocalError('')
    setError('')
    if (!name.trim()) {
      setLocalError(t('rolesPage.errNameRequired'))
      return
    }
    setSubmitting(true)
    try {
      await rolesAPI.create({
        name: name.trim(),
        description: description.trim() || undefined,
        groupId: groupId || undefined,
      })
      onSuccess()
    } catch (err) {
      const msg = getApiErrorMessage(err)
      setLocalError(msg)
      setError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={t('rolesPage.createRoleTitle')}
      size="md"
      footer={
        <>
          <button type="button" onClick={onClose} disabled={submitting} className="dashboard-btn-secondary">
            {t('ui.cancel')}
          </button>
          <button type="submit" form="role-create-form" disabled={submitting} className="dashboard-btn-primary disabled:opacity-70">
            {t('rolesPage.create')}
          </button>
        </>
      }
    >
      {localError && (
        <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
          {localError}
        </div>
      )}
      <form id="role-create-form" onSubmit={handleSubmit} className="space-y-4">
        <FormField label={t('rolesPage.nameLabel')} required>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="modal-input"
            required
          />
        </FormField>
        <FormField label={t('rolesPage.descriptionLabel')}>
          <input
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="modal-input"
          />
        </FormField>
        <FormField label={t('rolesPage.groupLabel')}>
          <select
            value={groupId}
            onChange={(e) => setGroupId(e.target.value)}
            className="modal-select"
          >
            <option value="">{t('rolesPage.groupNone')}</option>
            {groups.map((g) => (
              <option key={g.id} value={g.id}>{g.name}</option>
            ))}
          </select>
        </FormField>

      </form>
    </Modal>
  )
}

function DeleteRoleModal({
  role,
  roles,
  onClose,
  onSuccess,
  setError,
}: {
  role: RoleDto
  roles: RoleDto[]
  onClose: () => void
  onSuccess: () => void
  setError: (s: string) => void
}) {
  const { t } = useLanguage()
  const [targetRoleId, setTargetRoleId] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [localError, setLocalError] = useState('')
  const userCount = role.userCount ?? 0

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLocalError('')
    setError('')
    if (userCount > 0 && !targetRoleId) {
      setLocalError(t('rolesPage.errSelectReassign'))
      return
    }
    setSubmitting(true)
    try {
      await rolesAPI.delete(role.id, targetRoleId ? { targetRoleId } : undefined)
      onSuccess()
    } catch (err) {
      const msg = getApiErrorMessage(err)
      setLocalError(msg)
      setError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={t('rolesPage.deleteTitle')}
      description={`${t('rolesPage.deleteQuestion', { name: role.name })}${userCount > 0 ? ` ${t('rolesPage.reassignHint', { count: userCount })}` : ''}`}
      size="sm"
      footer={
        <>
          <button type="button" onClick={onClose} disabled={submitting} className="dashboard-btn-secondary">
            {t('ui.cancel')}
          </button>
          <button
            type="submit"
            form="role-delete-form"
            disabled={submitting}
            className="rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700 disabled:opacity-70"
          >
            {t('rolesPage.deleteConfirm')}
          </button>
        </>
      }
    >
      {localError && (
        <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
          {localError}
        </div>
      )}
      <form id="role-delete-form" onSubmit={handleSubmit} className="space-y-4">
        {userCount > 0 && (
          <FormField label={t('rolesPage.reassignLabel')} required>
            <select
              value={targetRoleId}
              onChange={(e) => setTargetRoleId(e.target.value)}
              className="modal-select"
              required
            >
              <option value="">{t('rolesPage.selectRole')}</option>
              {roles.map((r) => (
                <option key={r.id} value={r.id}>
                  {t('rolesPage.optionRoleUsers', { name: r.name, count: r.userCount ?? 0 })}
                </option>
              ))}
            </select>
          </FormField>
        )}
      </form>
    </Modal>
  )
}
