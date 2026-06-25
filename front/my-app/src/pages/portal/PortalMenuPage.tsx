import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, portalAPI, portalServicesAPI } from '../../services/api'
import type {
  RestaurantMenuDto,
  RestaurantMenuSectionDto,
  RestaurantMenuItemDto,
  CreateMenuSectionPayload,
  CreateMenuItemPayload,
} from '../../types/api'
import { Card } from '../../components/Card'
import { Modal } from '../../components/Modal'
import { ConfirmDialog } from '../../components/ConfirmDialog'

export function PortalMenuPage() {
  const { t } = useLanguage()
  const [serviceId, setServiceId] = useState<string | null>(null)

  const [menu, setMenu] = useState<RestaurantMenuDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  // Section modals
  const [addSectionOpen, setAddSectionOpen] = useState(false)
  const [editSection, setEditSection] = useState<RestaurantMenuSectionDto | null>(null)
  const [deleteSection, setDeleteSection] = useState<RestaurantMenuSectionDto | null>(null)
  const [sectionTitle, setSectionTitle] = useState('')
  const [sectionError, setSectionError] = useState('')

  // Item modals
  const [addItemSection, setAddItemSection] = useState<RestaurantMenuSectionDto | null>(null)
  const [editItem, setEditItem] = useState<{ item: RestaurantMenuItemDto; section: RestaurantMenuSectionDto } | null>(null)
  const [deleteItem, setDeleteItem] = useState<RestaurantMenuItemDto | null>(null)
  const [itemForm, setItemForm] = useState<CreateMenuItemPayload>({ name: '', description: '', price: undefined, currency: 'USD' })
  const [itemError, setItemError] = useState('')

  useEffect(() => {
    setLoading(true)
    portalAPI.listServices({ page: 0, size: 1 })
      .then((r) => {
        const page = r.data as { content?: Array<{ id: string }> }
        const sid = page?.content?.[0]?.id ?? null
        setServiceId(sid)
        if (!sid) { setError('No service found for this account.'); setLoading(false); return }
        return portalServicesAPI.getMenu(sid)
          .then((mr) => setMenu(mr.data))
          .catch((e) => setError(getApiErrorMessage(e)))
      })
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  // ── Section operations ─────────────────────────────────────────────────────

  function openAddSection() {
    setSectionTitle('')
    setSectionError('')
    setAddSectionOpen(true)
  }

  function openEditSection(section: RestaurantMenuSectionDto) {
    setSectionTitle(section.title)
    setSectionError('')
    setEditSection(section)
  }

  async function handleAddSection() {
    if (!serviceId || !sectionTitle.trim()) return
    setSaving(true)
    setSectionError('')
    const payload: CreateMenuSectionPayload = { title: sectionTitle.trim() }
    try {
      const res = await portalServicesAPI.createMenuSection(serviceId, payload)
      setMenu((prev) => prev ? { ...prev, sections: [...prev.sections, { ...res.data, items: [] }] } : prev)
      setAddSectionOpen(false)
    } catch (e) {
      setSectionError(getApiErrorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  async function handleEditSection() {
    if (!serviceId || !editSection) return
    setSaving(true)
    setSectionError('')
    try {
      const res = await portalServicesAPI.updateMenuSection(serviceId, editSection.id, { title: sectionTitle.trim() })
      setMenu((prev) => prev ? {
        ...prev,
        sections: prev.sections.map((s) => s.id === editSection.id ? { ...s, title: res.data.title } : s),
      } : prev)
      setEditSection(null)
    } catch (e) {
      setSectionError(getApiErrorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  async function handleDeleteSection() {
    if (!serviceId || !deleteSection) return
    setSaving(true)
    try {
      await portalServicesAPI.deleteMenuSection(serviceId, deleteSection.id)
      setMenu((prev) => prev ? { ...prev, sections: prev.sections.filter((s) => s.id !== deleteSection.id) } : prev)
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setSaving(false)
      setDeleteSection(null)
    }
  }

  // ── Item operations ────────────────────────────────────────────────────────

  function openAddItem(section: RestaurantMenuSectionDto) {
    setItemForm({ name: '', description: '', price: undefined, currency: 'USD' })
    setItemError('')
    setAddItemSection(section)
  }

  function openEditItem(item: RestaurantMenuItemDto, section: RestaurantMenuSectionDto) {
    setItemForm({ name: item.name, description: item.description ?? '', price: item.price, currency: item.currency ?? 'USD' })
    setItemError('')
    setEditItem({ item, section })
  }

  async function handleAddItem() {
    if (!serviceId || !addItemSection) return
    setSaving(true)
    setItemError('')
    try {
      const res = await portalServicesAPI.createMenuItem(serviceId, addItemSection.id, itemForm)
      setMenu((prev) => prev ? {
        ...prev,
        sections: prev.sections.map((s) =>
          s.id === addItemSection.id ? { ...s, items: [...s.items, res.data] } : s
        ),
      } : prev)
      setAddItemSection(null)
    } catch (e) {
      setItemError(getApiErrorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  async function handleEditItem() {
    if (!serviceId || !editItem) return
    setSaving(true)
    setItemError('')
    try {
      const res = await portalServicesAPI.updateMenuItem(serviceId, editItem.item.id, itemForm)
      setMenu((prev) => prev ? {
        ...prev,
        sections: prev.sections.map((s) =>
          s.id === editItem.section.id
            ? { ...s, items: s.items.map((i) => (i.id === editItem.item.id ? res.data : i)) }
            : s
        ),
      } : prev)
      setEditItem(null)
    } catch (e) {
      setItemError(getApiErrorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  async function handleDeleteItem() {
    if (!serviceId || !deleteItem) return
    setSaving(true)
    try {
      await portalServicesAPI.deleteMenuItem(serviceId, deleteItem.id)
      setMenu((prev) => prev ? {
        ...prev,
        sections: prev.sections.map((s) => ({
          ...s,
          items: s.items.filter((i) => i.id !== deleteItem.id),
        })),
      } : prev)
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setSaving(false)
      setDeleteItem(null)
    }
  }

  const inputCls =
    'w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800'

  function ItemForm({ formError }: { formError: string }) {
    return (
      <div className="space-y-3">
        {formError && <p className="text-sm text-red-500">{formError}</p>}
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.menuItemFieldName')}</label>
          <input className={inputCls} value={itemForm.name}
            onChange={(e) => setItemForm((f) => ({ ...f, name: e.target.value }))} />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.menuItemFieldDescription')}</label>
          <textarea className={inputCls} rows={2} value={itemForm.description ?? ''}
            onChange={(e) => setItemForm((f) => ({ ...f, description: e.target.value }))} />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.menuItemFieldPrice')}</label>
            <input type="number" min={0} step="0.01" className={inputCls} value={itemForm.price ?? ''}
              onChange={(e) => setItemForm((f) => ({ ...f, price: e.target.value ? Number(e.target.value) : undefined }))} />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.menuItemFieldCurrency')}</label>
            <input className={inputCls} value={itemForm.currency ?? 'USD'}
              onChange={(e) => setItemForm((f) => ({ ...f, currency: e.target.value }))} />
          </div>
        </div>
      </div>
    )
  }

  return (
    <>
      <div className="mb-4 flex items-center justify-between">
        <Link to="/portal/profile"
          className="text-sm text-slate-500 hover:text-slate-700 dark:hover:text-slate-300">
          ← {t('portalPages.profileTitle')}
        </Link>
        <button onClick={openAddSection} className="dashboard-btn-primary">
          {t('portalPages.menuSectionAdd')}
        </button>
      </div>

      <Card>
        <h1 className="mb-4 text-lg font-semibold">{t('portalPages.menuTitle')}</h1>

        {error && <p className="text-sm text-red-500">{error}</p>}
        {loading && <p className="text-sm text-slate-400">Loading…</p>}

        {!loading && menu?.sections.length === 0 && (
          <p className="text-sm text-slate-400">{t('portalPages.menuEmpty')}</p>
        )}

        <div className="space-y-4">
          {(menu?.sections ?? []).map((section) => (
            <div key={section.id} className="rounded-xl border border-slate-100 dark:border-slate-700">
              <div className="flex items-center justify-between rounded-t-xl bg-slate-50 px-4 py-2.5 dark:bg-slate-800/50">
                <span className="font-medium">{section.title}</span>
                <div className="flex gap-3 text-xs">
                  <button onClick={() => openEditSection(section)}
                    className="text-primary hover:underline">{t('portalPages.menuSectionEdit')}</button>
                  <button onClick={() => setDeleteSection(section)}
                    className="text-red-500 hover:underline">{t('portalPages.menuSectionDelete')}</button>
                </div>
              </div>

              <div className="divide-y divide-slate-50 px-4 dark:divide-slate-800">
                {section.items.length === 0 && (
                  <p className="py-3 text-xs text-slate-400">{t('portalPages.menuItemEmpty')}</p>
                )}
                {section.items.map((item) => (
                  <div key={item.id} className="flex items-center justify-between py-2.5">
                    <div>
                      <p className="text-sm font-medium">{item.name}</p>
                      {item.description && <p className="text-xs text-slate-400">{item.description}</p>}
                    </div>
                    <div className="flex items-center gap-4">
                      {item.price != null && (
                        <span className="text-sm font-semibold">
                          {item.price} {item.currency ?? ''}
                        </span>
                      )}
                      <div className="flex gap-2 text-xs">
                        <button onClick={() => openEditItem(item, section)}
                          className="text-primary hover:underline">{t('portalPages.menuItemEdit')}</button>
                        <button onClick={() => setDeleteItem(item)}
                          className="text-red-500 hover:underline">{t('portalPages.menuItemDelete')}</button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              <div className="px-4 pb-3 pt-1">
                <button onClick={() => openAddItem(section)}
                  className="text-xs text-primary hover:underline">
                  + {t('portalPages.menuItemAdd')}
                </button>
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* Section modals */}
      <Modal open={addSectionOpen} onClose={() => setAddSectionOpen(false)} title={t('portalPages.menuSectionAdd')}>
        <div className="space-y-3">
          {sectionError && <p className="text-sm text-red-500">{sectionError}</p>}
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.menuSectionFieldTitle')}</label>
            <input className={inputCls} value={sectionTitle} onChange={(e) => setSectionTitle(e.target.value)} />
          </div>
        </div>
        <div className="mt-4 flex justify-end gap-2">
          <button onClick={() => setAddSectionOpen(false)}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm">{t('ui.cancel')}</button>
          <button onClick={handleAddSection} disabled={saving}
            className="dashboard-btn-primary disabled:opacity-50">{saving ? t('portalPages.saving') : t('portalPages.save')}</button>
        </div>
      </Modal>

      <Modal open={!!editSection} onClose={() => setEditSection(null)} title={t('portalPages.menuSectionEdit')}>
        <div className="space-y-3">
          {sectionError && <p className="text-sm text-red-500">{sectionError}</p>}
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.menuSectionFieldTitle')}</label>
            <input className={inputCls} value={sectionTitle} onChange={(e) => setSectionTitle(e.target.value)} />
          </div>
        </div>
        <div className="mt-4 flex justify-end gap-2">
          <button onClick={() => setEditSection(null)}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm">{t('ui.cancel')}</button>
          <button onClick={handleEditSection} disabled={saving}
            className="dashboard-btn-primary disabled:opacity-50">{saving ? t('portalPages.saving') : t('portalPages.save')}</button>
        </div>
      </Modal>

      <ConfirmDialog
        open={!!deleteSection}
        onClose={() => setDeleteSection(null)}
        title={t('portalPages.menuSectionDelete')}
        description={t('portalPages.menuSectionDeleteConfirm')}
        confirmLabel={t('portalPages.menuSectionDelete')}
        variant="danger"
        onConfirm={handleDeleteSection}
      />

      {/* Item modals */}
      <Modal open={!!addItemSection} onClose={() => setAddItemSection(null)} title={t('portalPages.menuItemAdd')}>
        <ItemForm formError={itemError} />
        <div className="mt-4 flex justify-end gap-2">
          <button onClick={() => setAddItemSection(null)}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm">{t('ui.cancel')}</button>
          <button onClick={handleAddItem} disabled={saving}
            className="dashboard-btn-primary disabled:opacity-50">{saving ? t('portalPages.saving') : t('portalPages.save')}</button>
        </div>
      </Modal>

      <Modal open={!!editItem} onClose={() => setEditItem(null)} title={t('portalPages.menuItemEdit')}>
        <ItemForm formError={itemError} />
        <div className="mt-4 flex justify-end gap-2">
          <button onClick={() => setEditItem(null)}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm">{t('ui.cancel')}</button>
          <button onClick={handleEditItem} disabled={saving}
            className="dashboard-btn-primary disabled:opacity-50">{saving ? t('portalPages.saving') : t('portalPages.save')}</button>
        </div>
      </Modal>

      <ConfirmDialog
        open={!!deleteItem}
        onClose={() => setDeleteItem(null)}
        title={t('portalPages.menuItemDelete')}
        description={t('portalPages.menuItemDeleteConfirm')}
        confirmLabel={t('portalPages.menuItemDelete')}
        variant="danger"
        onConfirm={handleDeleteItem}
      />
    </>
  )
}
