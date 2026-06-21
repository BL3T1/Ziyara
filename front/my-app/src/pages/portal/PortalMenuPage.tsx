import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { getApiErrorMessage, portalServicesAPI } from '../../services/api'
import type {
  RestaurantMenuDto,
  RestaurantMenuSectionDto,
  RestaurantMenuItemDto,
  CreateMenuSectionPayload,
  CreateMenuItemPayload,
  UpdateMenuItemPayload,
} from '../../types/api'
import { Card } from '../../components/Card'
import { Modal } from '../../components/Modal'
import { ConfirmDialog } from '../../components/ConfirmDialog'

type ItemFormState = {
  name: string
  description: string
  price: string
  currency: string
  imageUrl: string
}

const EMPTY_ITEM_FORM: ItemFormState = {
  name: '',
  description: '',
  price: '',
  currency: 'USD',
  imageUrl: '',
}

function itemToForm(item: RestaurantMenuItemDto): ItemFormState {
  return {
    name: item.name,
    description: item.description ?? '',
    price: item.price != null ? String(item.price) : '',
    currency: item.currency ?? 'USD',
    imageUrl: item.imageUrl ?? '',
  }
}

function ItemFormFields({
  form,
  onChange,
  inputCls,
}: {
  form: ItemFormState
  onChange: (patch: Partial<ItemFormState>) => void
  inputCls: string
}) {
  const { t } = useLanguage()
  return (
    <div className="space-y-3">
      <div>
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
          {t('portalPages.menuFieldName')} *
        </label>
        <input
          required
          value={form.name}
          onChange={(e) => onChange({ name: e.target.value })}
          className={inputCls}
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
          {t('portalPages.menuFieldDescription')}
        </label>
        <textarea
          rows={2}
          value={form.description}
          onChange={(e) => onChange({ description: e.target.value })}
          className={inputCls}
        />
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('portalPages.menuFieldPrice')}
          </label>
          <input
            type="number"
            min={0}
            step="0.01"
            value={form.price}
            onChange={(e) => onChange({ price: e.target.value })}
            className={inputCls}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('portalPages.menuFieldCurrency')}
          </label>
          <input
            maxLength={3}
            value={form.currency}
            onChange={(e) => onChange({ currency: e.target.value.toUpperCase() })}
            className={inputCls}
          />
        </div>
      </div>
      <div>
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
          {t('portalPages.menuFieldImageUrl')}
        </label>
        <input
          type="url"
          value={form.imageUrl}
          onChange={(e) => onChange({ imageUrl: e.target.value })}
          placeholder="https://…"
          className={inputCls}
        />
      </div>
    </div>
  )
}

function formToItemPayload(f: ItemFormState): CreateMenuItemPayload {
  return {
    name: f.name.trim(),
    description: f.description.trim() || undefined,
    price: f.price ? parseFloat(f.price) : undefined,
    currency: f.currency.trim() || 'USD',
    imageUrl: f.imageUrl.trim() || undefined,
  }
}

export function PortalMenuPage() {
  const { id: serviceId } = useParams<{ id: string }>()
  const { t } = useLanguage()
  useDocumentMeta({ title: t('portalPages.menuPageTitle') })

  const [menu, setMenu] = useState<RestaurantMenuDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Section modals
  const [addSectionOpen, setAddSectionOpen] = useState(false)
  const [addSectionTitle, setAddSectionTitle] = useState('')
  const [addSectionSubmitting, setAddSectionSubmitting] = useState(false)
  const [addSectionError, setAddSectionError] = useState<string | null>(null)

  const [editSection, setEditSection] = useState<RestaurantMenuSectionDto | null>(null)
  const [editSectionTitle, setEditSectionTitle] = useState('')
  const [editSectionSubmitting, setEditSectionSubmitting] = useState(false)
  const [editSectionError, setEditSectionError] = useState<string | null>(null)

  const [deleteSection, setDeleteSection] = useState<RestaurantMenuSectionDto | null>(null)

  // Item modals — track which section the add/edit/delete targets
  const [addItemSection, setAddItemSection] = useState<RestaurantMenuSectionDto | null>(null)
  const [addItemForm, setAddItemForm] = useState<ItemFormState>(EMPTY_ITEM_FORM)
  const [addItemSubmitting, setAddItemSubmitting] = useState(false)
  const [addItemError, setAddItemError] = useState<string | null>(null)

  const [editItem, setEditItem] = useState<{ item: RestaurantMenuItemDto; serviceId: string } | null>(null)
  const [editItemForm, setEditItemForm] = useState<ItemFormState>(EMPTY_ITEM_FORM)
  const [editItemSubmitting, setEditItemSubmitting] = useState(false)
  const [editItemError, setEditItemError] = useState<string | null>(null)

  const [deleteItem, setDeleteItem] = useState<RestaurantMenuItemDto | null>(null)

  const inputCls =
    'mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100'

  function load() {
    if (!serviceId) return
    setLoading(true)
    portalServicesAPI
      .getMenu(serviceId)
      .then((res) => setMenu(res.data as RestaurantMenuDto))
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }

  useEffect(load, [serviceId])

  // ── Section handlers ──────────────────────────────────────────────────────

  async function handleAddSection(e: React.FormEvent) {
    e.preventDefault()
    if (!serviceId) return
    setAddSectionError(null)
    setAddSectionSubmitting(true)
    try {
      const payload: CreateMenuSectionPayload = { title: addSectionTitle.trim() }
      const created = (await portalServicesAPI.createMenuSection(serviceId, payload)).data as RestaurantMenuSectionDto
      setMenu((prev) =>
        prev ? { ...prev, sections: [...prev.sections, { ...created, items: [] }] } : prev,
      )
      setAddSectionOpen(false)
      setAddSectionTitle('')
    } catch (err) {
      setAddSectionError(getApiErrorMessage(err))
    } finally {
      setAddSectionSubmitting(false)
    }
  }

  async function handleEditSection(e: React.FormEvent) {
    e.preventDefault()
    if (!serviceId || !editSection) return
    setEditSectionError(null)
    setEditSectionSubmitting(true)
    try {
      const updated = (
        await portalServicesAPI.updateMenuSection(serviceId, editSection.id, { title: editSectionTitle.trim() })
      ).data as RestaurantMenuSectionDto
      setMenu((prev) =>
        prev
          ? {
              ...prev,
              sections: prev.sections.map((s) =>
                s.id === updated.id ? { ...s, title: updated.title } : s,
              ),
            }
          : prev,
      )
      setEditSection(null)
    } catch (err) {
      setEditSectionError(getApiErrorMessage(err))
    } finally {
      setEditSectionSubmitting(false)
    }
  }

  async function handleDeleteSection() {
    if (!serviceId || !deleteSection) return
    try {
      await portalServicesAPI.deleteMenuSection(serviceId, deleteSection.id)
      setMenu((prev) =>
        prev ? { ...prev, sections: prev.sections.filter((s) => s.id !== deleteSection.id) } : prev,
      )
      setDeleteSection(null)
    } catch (err) {
      setError(getApiErrorMessage(err))
    }
  }

  // ── Item handlers ─────────────────────────────────────────────────────────

  async function handleAddItem(e: React.FormEvent) {
    e.preventDefault()
    if (!serviceId || !addItemSection) return
    setAddItemError(null)
    setAddItemSubmitting(true)
    try {
      const created = (
        await portalServicesAPI.createMenuItem(serviceId, addItemSection.id, formToItemPayload(addItemForm))
      ).data as RestaurantMenuItemDto
      setMenu((prev) =>
        prev
          ? {
              ...prev,
              sections: prev.sections.map((s) =>
                s.id === addItemSection.id ? { ...s, items: [...s.items, created] } : s,
              ),
            }
          : prev,
      )
      setAddItemSection(null)
      setAddItemForm(EMPTY_ITEM_FORM)
    } catch (err) {
      setAddItemError(getApiErrorMessage(err))
    } finally {
      setAddItemSubmitting(false)
    }
  }

  async function handleEditItem(e: React.FormEvent) {
    e.preventDefault()
    if (!serviceId || !editItem) return
    setEditItemError(null)
    setEditItemSubmitting(true)
    try {
      const payload: UpdateMenuItemPayload = formToItemPayload(editItemForm)
      const updated = (
        await portalServicesAPI.updateMenuItem(serviceId, editItem.item.id, payload)
      ).data as RestaurantMenuItemDto
      setMenu((prev) =>
        prev
          ? {
              ...prev,
              sections: prev.sections.map((s) => ({
                ...s,
                items: s.items.map((i) => (i.id === updated.id ? updated : i)),
              })),
            }
          : prev,
      )
      setEditItem(null)
    } catch (err) {
      setEditItemError(getApiErrorMessage(err))
    } finally {
      setEditItemSubmitting(false)
    }
  }

  async function handleDeleteItem() {
    if (!serviceId || !deleteItem) return
    try {
      await portalServicesAPI.deleteMenuItem(serviceId, deleteItem.id)
      setMenu((prev) =>
        prev
          ? {
              ...prev,
              sections: prev.sections.map((s) => ({
                ...s,
                items: s.items.filter((i) => i.id !== deleteItem.id),
              })),
            }
          : prev,
      )
      setDeleteItem(null)
    } catch (err) {
      setError(getApiErrorMessage(err))
    }
  }

  if (!serviceId) return null

  return (
    <>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <Link
            to={`/portal/listings/${serviceId}`}
            className="text-sm font-medium text-primary hover:underline"
          >
            {t('portalPages.menuBackToListing')}
          </Link>
          <h1 className="mt-3 app-page-title">{t('portalPages.menuPageTitle')}</h1>
        </div>
        <button
          type="button"
          onClick={() => { setAddSectionTitle(''); setAddSectionError(null); setAddSectionOpen(true) }}
          className="dashboard-btn-primary"
        >
          {t('portalPages.menuAddSection')}
        </button>
      </div>

      {error && (
        <p className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
          {error}
        </p>
      )}

      {loading ? (
        <p className="text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
      ) : !menu || menu.sections.length === 0 ? (
        <Card className="p-6 text-center text-sm text-slate-500 dark:text-slate-400">
          {t('portalPages.menuNoSections')}
        </Card>
      ) : (
        <div className="space-y-4">
          {menu.sections.map((section) => (
            <Card key={section.id} className="p-0 overflow-hidden">
              {/* Section header */}
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 px-5 py-3 dark:border-slate-700">
                <span className="font-semibold text-slate-800 dark:text-slate-100">{section.title}</span>
                <div className="flex gap-3">
                  <button
                    type="button"
                    onClick={() => {
                      setEditSection(section)
                      setEditSectionTitle(section.title)
                      setEditSectionError(null)
                    }}
                    className="text-xs font-medium text-primary hover:underline"
                  >
                    {t('portalPages.menuEditSection')}
                  </button>
                  <button
                    type="button"
                    onClick={() => setDeleteSection(section)}
                    className="text-xs font-medium text-red-600 hover:underline dark:text-red-400"
                  >
                    {t('portalPages.menuDeleteSection')}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setAddItemSection(section)
                      setAddItemForm(EMPTY_ITEM_FORM)
                      setAddItemError(null)
                    }}
                    className="text-xs font-medium text-emerald-600 hover:underline dark:text-emerald-400"
                  >
                    + {t('portalPages.menuAddItem')}
                  </button>
                </div>
              </div>

              {/* Items */}
              {section.items.length === 0 ? (
                <p className="px-5 py-4 text-sm text-slate-400 dark:text-slate-500">
                  {t('portalPages.menuNoItems')}
                </p>
              ) : (
                <div className="divide-y divide-slate-100 dark:divide-slate-700/60">
                  {section.items.map((item) => (
                    <div key={item.id} className="flex items-start justify-between gap-4 px-5 py-3">
                      <div className="min-w-0">
                        <p className="font-medium text-slate-800 dark:text-slate-100">{item.name}</p>
                        {item.description && (
                          <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400 line-clamp-2">
                            {item.description}
                          </p>
                        )}
                        {item.price != null && (
                          <p className="mt-0.5 text-xs font-semibold text-primary dark:text-blue-300">
                            {item.price} {item.currency}
                          </p>
                        )}
                      </div>
                      <div className="flex shrink-0 gap-2">
                        <button
                          type="button"
                          onClick={() => {
                            setEditItem({ item, serviceId })
                            setEditItemForm(itemToForm(item))
                            setEditItemError(null)
                          }}
                          className="text-xs font-medium text-primary hover:underline"
                        >
                          {t('portalPages.menuEditItem')}
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleteItem(item)}
                          className="text-xs font-medium text-red-600 hover:underline dark:text-red-400"
                        >
                          {t('portalPages.menuDeleteItem')}
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          ))}
        </div>
      )}

      {/* Add Section Modal */}
      <Modal
        open={addSectionOpen}
        onClose={() => setAddSectionOpen(false)}
        title={t('portalPages.menuAddSection')}
        size="sm"
        footer={
          <div className="flex justify-end gap-3">
            <button type="button" onClick={() => setAddSectionOpen(false)} className="dashboard-btn-secondary">
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="add-section-form"
              disabled={addSectionSubmitting}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {addSectionSubmitting ? t('ui.loading') : t('ui.save')}
            </button>
          </div>
        }
      >
        <form id="add-section-form" onSubmit={handleAddSection}>
          {addSectionError && (
            <p className="mb-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
              {addSectionError}
            </p>
          )}
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('portalPages.menuSectionTitle')} *
          </label>
          <input
            required
            value={addSectionTitle}
            onChange={(e) => setAddSectionTitle(e.target.value)}
            placeholder="e.g. Starters, Mains, Desserts"
            className={inputCls}
          />
        </form>
      </Modal>

      {/* Edit Section Modal */}
      <Modal
        open={!!editSection}
        onClose={() => setEditSection(null)}
        title={t('portalPages.menuEditSection')}
        size="sm"
        footer={
          <div className="flex justify-end gap-3">
            <button type="button" onClick={() => setEditSection(null)} className="dashboard-btn-secondary">
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="edit-section-form"
              disabled={editSectionSubmitting}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {editSectionSubmitting ? t('ui.loading') : t('ui.save')}
            </button>
          </div>
        }
      >
        <form id="edit-section-form" onSubmit={handleEditSection}>
          {editSectionError && (
            <p className="mb-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
              {editSectionError}
            </p>
          )}
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('portalPages.menuSectionTitle')} *
          </label>
          <input
            required
            value={editSectionTitle}
            onChange={(e) => setEditSectionTitle(e.target.value)}
            className={inputCls}
          />
        </form>
      </Modal>

      {/* Delete Section Confirm */}
      <ConfirmDialog
        open={!!deleteSection}
        onClose={() => setDeleteSection(null)}
        title={t('portalPages.menuDeleteSection')}
        description={t('portalPages.menuConfirmDeleteSection')}
        confirmLabel={t('portalPages.menuDeleteSection')}
        variant="danger"
        onConfirm={handleDeleteSection}
      />

      {/* Add Item Modal */}
      <Modal
        open={!!addItemSection}
        onClose={() => setAddItemSection(null)}
        title={t('portalPages.menuAddItem')}
        size="md"
        footer={
          <div className="flex justify-end gap-3">
            <button type="button" onClick={() => setAddItemSection(null)} className="dashboard-btn-secondary">
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="add-item-form"
              disabled={addItemSubmitting}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {addItemSubmitting ? t('ui.loading') : t('ui.save')}
            </button>
          </div>
        }
      >
        <form id="add-item-form" onSubmit={handleAddItem}>
          {addItemError && (
            <p className="mb-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
              {addItemError}
            </p>
          )}
          <ItemFormFields
            form={addItemForm}
            onChange={(p) => setAddItemForm((f) => ({ ...f, ...p }))}
            inputCls={inputCls}
          />
        </form>
      </Modal>

      {/* Edit Item Modal */}
      <Modal
        open={!!editItem}
        onClose={() => setEditItem(null)}
        title={t('portalPages.menuEditItem')}
        size="md"
        footer={
          <div className="flex justify-end gap-3">
            <button type="button" onClick={() => setEditItem(null)} className="dashboard-btn-secondary">
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="edit-item-form"
              disabled={editItemSubmitting}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {editItemSubmitting ? t('ui.loading') : t('ui.save')}
            </button>
          </div>
        }
      >
        <form id="edit-item-form" onSubmit={handleEditItem}>
          {editItemError && (
            <p className="mb-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
              {editItemError}
            </p>
          )}
          <ItemFormFields
            form={editItemForm}
            onChange={(p) => setEditItemForm((f) => ({ ...f, ...p }))}
            inputCls={inputCls}
          />
        </form>
      </Modal>

      {/* Delete Item Confirm */}
      <ConfirmDialog
        open={!!deleteItem}
        onClose={() => setDeleteItem(null)}
        title={t('portalPages.menuDeleteItem')}
        description={t('portalPages.menuConfirmDeleteItem')}
        confirmLabel={t('portalPages.menuDeleteItem')}
        variant="danger"
        onConfirm={handleDeleteItem}
      />
    </>
  )
}
