/**
 * Admin > Roles – list roles, create, edit permissions, delete with reassignment.
 */

import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { rolesAPI, getApiErrorMessage } from '../../services/api'
import type { RoleDto, GroupDto, PermissionCatalogueItemDto } from '../../types/api'
import { ALL_SIDEBAR_ITEM_IDS, SIDEBAR_SECTIONS } from '../../config/sidebar'

/** Stable resource groups for permission catalogue UI */
function catalogueByResource(items: PermissionCatalogueItemDto[]): [string, PermissionCatalogueItemDto[]][] {
  const map = new Map<string, PermissionCatalogueItemDto[]>()
  const sorted = [...items].sort((a, b) => {
    const ra = a.resource ?? ''
    const rb = b.resource ?? ''
    if (ra !== rb) return ra.localeCompare(rb)
    return (a.code ?? '').localeCompare(b.code ?? '')
  })
  for (const p of sorted) {
    const key = p.resource?.trim() || 'other'
    const list = map.get(key) ?? []
    list.push(p)
    map.set(key, list)
  }
  return Array.from(map.entries())
}

export function RolesPage() {
  const { t } = useLanguage()
  const [searchParams, setSearchParams] = useSearchParams()
  const [roles, setRoles] = useState<RoleDto[]>([])
  const [groups, setGroups] = useState<GroupDto[]>([])
  const [catalogue, setCatalogue] = useState<PermissionCatalogueItemDto[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [editPermissionsRole, setEditPermissionsRole] = useState<RoleDto | null>(null)
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
      setEditPermissionsRole(match)
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
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="dashboard-btn-primary shrink-0"
        >
          {t('rolesPage.createRoleButton')}
        </button>
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="mt-6 table-shell">
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
            {roles.map((role) => (
              <tr key={role.id}>
                <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-slate-900 dark:text-slate-100">{role.name}</td>
                <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{role.code ?? '—'}</td>
                <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{role.userCount ?? 0}</td>
                <td className="whitespace-nowrap px-4 py-3 text-right text-sm">
                  <button
                    type="button"
                    onClick={() => setEditPermissionsRole(role)}
                    className="text-primary hover:underline"
                  >
                    {t('rolesPage.editPermissions')}
                  </button>
                  <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                  <button
                    type="button"
                    onClick={() => setEditNavRole(role)}
                    className="text-primary hover:underline"
                  >
                    {t('rolesPage.editSidebarNav')}
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
          catalogue={catalogue}
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
          onClose={() => setEditDetailsRole(null)}
          onSuccess={() => {
            setEditDetailsRole(null)
            setError('')
            load()
          }}
          setError={setError}
        />
      )}

      {editPermissionsRole && (
        <EditPermissionsModal
          role={editPermissionsRole}
          catalogue={catalogue}
          onClose={() => setEditPermissionsRole(null)}
          onSuccess={() => {
            setEditPermissionsRole(null)
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

function EditRoleNavigationModal({
  role,
  onClose,
  onSuccess,
  setError,
}: {
  role: RoleDto
  onClose: () => void
  onSuccess: () => void
  setError: (s: string) => void
}) {
  const { t } = useLanguage()
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [localError, setLocalError] = useState('')

  useEffect(() => {
    rolesAPI
      .get(role.id)
      .then((r) => {
        const data = r.data as RoleDto
        const ids = data?.navigationItemIds
        setSelected(ids?.length ? new Set(ids) : new Set())
      })
      .catch(() => setSelected(new Set()))
      .finally(() => setLoading(false))
  }, [role.id])

  const toggle = (id: string) => {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const orderedVisibleIds = () => ALL_SIDEBAR_ITEM_IDS.filter((id) => selected.has(id))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLocalError('')
    setError('')
    setSubmitting(true)
    try {
      await rolesAPI.updateNavigation(role.id, { visibleItemIds: orderedVisibleIds() })
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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-700 dark:bg-slate-800"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">{t('rolesPage.editNavTitle', { name: role.name })}</h3>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t('rolesPage.editNavHint')}</p>
        {localError && (
          <div className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
            {localError}
          </div>
        )}
        {loading ? (
          <div className="mt-4 py-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : (
          <form onSubmit={handleSubmit} className="mt-4 space-y-4">
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => setSelected(new Set(ALL_SIDEBAR_ITEM_IDS))}
                className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 dark:border-slate-600 dark:text-slate-300"
              >
                {t('rolesPage.navPresetFull')}
              </button>
              <button
                type="button"
                onClick={() => setSelected(new Set())}
                className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 dark:border-slate-600 dark:text-slate-300"
              >
                {t('rolesPage.navPresetClear')}
              </button>
            </div>
            <div className="max-h-[50vh] space-y-4 overflow-y-auto rounded-lg border border-slate-200 p-3 dark:border-slate-600">
              {SIDEBAR_SECTIONS.map((section) => (
                <div key={section.id}>
                  <div className="mb-2 text-xs font-bold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                    {t(`section.${section.id}`)}
                  </div>
                  <div className="space-y-1.5">
                    {section.items.map((item) => (
                      <label key={item.id} className="flex cursor-pointer items-center gap-2 py-0.5">
                        <input
                          type="checkbox"
                          checked={selected.has(item.id)}
                          onChange={() => toggle(item.id)}
                          className="rounded border-slate-300 text-primary"
                        />
                        <span className="text-sm text-slate-700 dark:text-slate-200">{t(`nav.${item.id}`)}</span>
                      </label>
                    ))}
                  </div>
                </div>
              ))}
            </div>
            <div className="flex gap-3 pt-2">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-700"
              >
                {t('ui.cancel')}
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="dashboard-btn-primary flex-1 disabled:opacity-70"
              >
                {t('rolesPage.save')}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}

function EditRoleDetailsModal({
  role,
  onClose,
  onSuccess,
  setError,
}: {
  role: RoleDto
  onClose: () => void
  onSuccess: () => void
  setError: (s: string) => void
}) {
  const { t } = useLanguage()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [nameAr, setNameAr] = useState('')
  const [descriptionAr, setDescriptionAr] = useState('')
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
      })
      .catch(() => {
        setName(role.name ?? '')
        setDescription('')
        setNameAr('')
        setDescriptionAr('')
      })
      .finally(() => setLoading(false))
  }, [role.id, role.name])

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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-700 dark:bg-slate-800"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">
          {t('rolesPage.editDetailsTitle', { name: role.name })}
        </h3>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t('rolesPage.editDetailsHint')}</p>
        {localError && (
          <div className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
            {localError}
          </div>
        )}
        {loading ? (
          <div className="mt-4 py-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : (
          <form onSubmit={handleSubmit} className="mt-4 space-y-4">
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('rolesPage.nameLabel')}</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                required
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('rolesPage.descriptionLabel')}</label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('rolesPage.nameArLabel')}</label>
              <input
                type="text"
                value={nameAr}
                onChange={(e) => setNameAr(e.target.value)}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                dir="rtl"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('rolesPage.descriptionArLabel')}</label>
              <input
                type="text"
                value={descriptionAr}
                onChange={(e) => setDescriptionAr(e.target.value)}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                dir="rtl"
              />
            </div>
            <div className="flex gap-3 pt-2">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-700"
              >
                {t('ui.cancel')}
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="dashboard-btn-primary flex-1 disabled:opacity-70"
              >
                {t('rolesPage.save')}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}

function CreateRoleModal({
  groups,
  catalogue,
  onClose,
  onSuccess,
  setError,
}: {
  groups: GroupDto[]
  catalogue: PermissionCatalogueItemDto[]
  onClose: () => void
  onSuccess: () => void
  setError: (s: string) => void
}) {
  const { t } = useLanguage()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [groupId, setGroupId] = useState('')
  const [permissionIds, setPermissionIds] = useState<string[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [localError, setLocalError] = useState('')

  const unlocked = catalogue.filter((p) => !p.locked)
  const locked = catalogue.filter((p) => p.locked)

  const togglePermission = (id: string) => {
    setPermissionIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    )
  }

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
        permissionIds: permissionIds.length ? permissionIds : undefined,
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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-700 dark:bg-slate-800"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">{t('rolesPage.createRoleTitle')}</h3>
        {localError && (
          <div className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
            {localError}
          </div>
        )}
        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('rolesPage.nameLabel')}</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              required
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('rolesPage.descriptionLabel')}</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('rolesPage.groupLabel')}</label>
            <select
              value={groupId}
              onChange={(e) => setGroupId(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            >
              <option value="">{t('rolesPage.groupNone')}</option>
              {groups.map((g) => (
                <option key={g.id} value={g.id}>
                  {g.name}
                </option>
              ))}
            </select>
          </div>
          {unlocked.length > 0 && (
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('rolesPage.permissionsOptional')}</label>
              <div className="max-h-40 overflow-y-auto rounded-lg border border-slate-200 p-2 dark:border-slate-600">
                {unlocked.map((p) => (
                  <label key={p.id} className="flex cursor-pointer items-center gap-2 py-1">
                    <input
                      type="checkbox"
                      checked={permissionIds.includes(p.id)}
                      onChange={() => togglePermission(p.id)}
                      className="rounded border-slate-300 text-primary"
                    />
                    <span className="text-sm text-slate-700 dark:text-slate-200">{p.name ?? p.code}</span>
                  </label>
                ))}
              </div>
            </div>
          )}
          {locked.length > 0 && (
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-600 dark:text-slate-300">{t('rolesPage.createRoleLockedReference')}</label>
              <div className="max-h-32 overflow-y-auto rounded-lg border border-dashed border-slate-200 bg-slate-50 p-2 dark:border-slate-600 dark:bg-slate-900/40">
                {locked.map((p) => (
                  <div key={p.id} className="flex items-center gap-2 py-1 opacity-70">
                    <input type="checkbox" disabled checked={false} className="rounded border-slate-300" readOnly aria-hidden />
                    <span className="text-sm text-slate-600 dark:text-slate-400">
                      {p.name ?? p.code}{' '}
                      <span className="text-xs text-amber-700 dark:text-amber-400">({t('rolesPage.permLockedBadge')})</span>
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="flex-1 rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-700">
              {t('ui.cancel')}
            </button>
            <button type="submit" disabled={submitting} className="dashboard-btn-primary flex-1 disabled:opacity-70">
              {t('rolesPage.create')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function EditPermissionsModal({
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
  const [permissionIds, setPermissionIds] = useState<string[]>(role.permissions ?? [])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [localError, setLocalError] = useState('')

  useEffect(() => {
    rolesAPI
      .get(role.id)
      .then((r) => {
        const data = r.data as RoleDto & { permissionIds?: string[] }
        const raw = data?.permissionIds ?? data?.permissions ?? []
        const sys = data?.systemRole === true
        setPermissionIds(
          sys
            ? raw
            : raw.filter((id) => !catalogue.some((c) => c.id === id && c.locked)),
        )
      })
      .catch(() => setPermissionIds([]))
      .finally(() => setLoading(false))
    // catalogue is stable for the session once Roles page loaded; avoid re-fetch loop on parent re-renders
    // eslint-disable-next-line react-hooks/exhaustive-deps -- only reload when switching role
  }, [role.id])

  const isSystemRole = role.systemRole === true
  const groups = catalogueByResource(catalogue)

  const togglePermission = (id: string, canToggle: boolean) => {
    if (!canToggle) return
    setPermissionIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    )
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLocalError('')
    setError('')
    setSubmitting(true)
    try {
      const lockedIds = new Set(catalogue.filter((p) => p.locked).map((p) => p.id))
      const toSend =
        isSystemRole ? permissionIds : permissionIds.filter((id) => !lockedIds.has(id))
      await rolesAPI.updatePermissions(role.id, { permissionIds: toSend })
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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-700 dark:bg-slate-800"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">{t('rolesPage.editPermTitle', { name: role.name })}</h3>
        {localError && (
          <div className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
            {localError}
          </div>
        )}
        {loading ? (
          <div className="mt-4 py-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : (
          <form onSubmit={handleSubmit} className="mt-4 space-y-4">
            <p className="text-sm text-slate-600 dark:text-slate-300">
              {isSystemRole ? t('rolesPage.permHintSystemRole') : t('rolesPage.permHintCustomRole')}
            </p>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('rolesPage.permissionsLabel')}</label>
              <div className="max-h-[min(24rem,55vh)] space-y-4 overflow-y-auto rounded-lg border border-slate-200 p-3 dark:border-slate-600">
                {catalogue.length === 0 ? (
                  <p className="text-sm text-slate-500 dark:text-slate-400">{t('rolesPage.noPermissionsInCatalogue')}</p>
                ) : (
                  groups.map(([resource, perms]) => (
                    <div key={resource}>
                      <div className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">{resource}</div>
                      <div className="space-y-1 pl-0">
                        {perms.map((p) => {
                          const locked = Boolean(p.locked)
                          const canToggle = isSystemRole || !locked
                          return (
                            <label
                              key={p.id}
                              className={`flex items-center gap-2 py-1 ${canToggle ? 'cursor-pointer' : 'cursor-not-allowed opacity-80'}`}
                            >
                              <input
                                type="checkbox"
                                checked={permissionIds.includes(p.id)}
                                disabled={!canToggle}
                                onChange={() => togglePermission(p.id, canToggle)}
                                className="rounded border-slate-300 text-primary disabled:opacity-50"
                              />
                              <span className="text-sm text-slate-700 dark:text-slate-200">
                                {p.name ?? p.code}
                                {locked ? (
                                  <span className="ms-1 text-xs text-amber-700 dark:text-amber-400">({t('rolesPage.permLockedBadge')})</span>
                                ) : null}
                              </span>
                            </label>
                          )
                        })}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
            <div className="flex gap-3 pt-2">
              <button type="button" onClick={onClose} className="flex-1 rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-700">
                {t('ui.cancel')}
              </button>
              <button type="submit" disabled={submitting} className="dashboard-btn-primary flex-1 disabled:opacity-70">
                {t('rolesPage.save')}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div
        className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-700 dark:bg-slate-800"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">{t('rolesPage.deleteTitle')}</h3>
        <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">
          {t('rolesPage.deleteQuestion', { name: role.name })}
          {userCount > 0 ? ` ${t('rolesPage.reassignHint', { count: userCount })}` : ''}
        </p>
        {localError && (
          <div className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
            {localError}
          </div>
        )}
        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          {userCount > 0 && (
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('rolesPage.reassignLabel')}</label>
              <select
                value={targetRoleId}
                onChange={(e) => setTargetRoleId(e.target.value)}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                required={userCount > 0}
              >
                <option value="">{t('rolesPage.selectRole')}</option>
                {roles.map((r) => (
                  <option key={r.id} value={r.id}>
                    {t('rolesPage.optionRoleUsers', { name: r.name, count: r.userCount ?? 0 })}
                  </option>
                ))}
              </select>
            </div>
          )}
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="flex-1 rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-700">
              {t('ui.cancel')}
            </button>
            <button type="submit" disabled={submitting} className="flex-1 rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-70">
              {t('rolesPage.deleteConfirm')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
