/**
 * Company staff or provider: manage service images (URL) and restaurant menu from the detail page.
 */

import { useCallback, useEffect, useRef, useState } from 'react'
import { getApiErrorMessage, portalServicesAPI, servicesAPI } from '../../services/api'
import { Card } from '../../components/Card'
import type {
  CreateMenuItemPayload,
  CreateHotelRoomPayload,
  CreateServiceImagePayload,
  HotelRoomDto,
  RestaurantMenuDto,
  RestaurantMenuItemDto,
  ServiceImageCategoryDto,
  ServiceImageDto,
  ServiceTypeDto,
  UpdateHotelRoomPayload,
  UpdateMenuItemPayload,
  UpdateServiceImagePayload,
} from '../../types/api'

const IMAGE_CATEGORIES: ServiceImageCategoryDto[] = ['PROPERTY', 'ROOM', 'TRIP', 'OTHER']

type Variant = 'company' | 'provider'

function mediaApi(variant: Variant) {
  return variant === 'company' ? servicesAPI : portalServicesAPI
}

const inputClass =
  'mt-1 w-full rounded-md border border-slate-300 bg-white px-2 py-1.5 text-sm text-slate-900 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100'
const labelClass = 'block text-xs font-medium text-slate-600 dark:text-slate-400'
const btnPrimary = 'dashboard-btn-primary rounded-md px-3 py-1.5 text-sm disabled:opacity-50'
const btnSecondary =
  'dashboard-btn-secondary rounded-md px-2 py-1 text-xs font-medium dark:hover:bg-slate-800'
const btnDanger =
  'rounded-md border border-red-300 px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-50 dark:border-red-800 dark:text-red-300 dark:hover:bg-red-950/40'

interface Props {
  serviceId: string
  serviceType: ServiceTypeDto
  currency: string
  images: ServiceImageDto[]
  menu: RestaurantMenuDto | null
  variant: Variant
  onRefresh: () => Promise<void>
}

export function ServiceDetailMediaEditor({
  serviceId,
  serviceType,
  currency,
  images,
  menu,
  variant,
  onRefresh,
}: Props) {
  const api = mediaApi(variant)
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)

  const [newImage, setNewImage] = useState<CreateServiceImagePayload>({
    url: '',
    altText: '',
    category: 'PROPERTY',
    contextKey: '',
    primary: false,
  })

  const [editingImageId, setEditingImageId] = useState<string | null>(null)
  const [editImageForm, setEditImageForm] = useState<UpdateServiceImagePayload>({})

  const [newSectionTitle, setNewSectionTitle] = useState('')

  const [newItemBySection, setNewItemBySection] = useState<Record<string, CreateMenuItemPayload>>({})
  const [editingItemId, setEditingItemId] = useState<string | null>(null)
  const [editItemForm, setEditItemForm] = useState<UpdateMenuItemPayload>({})
  const [rooms, setRooms] = useState<HotelRoomDto[]>([])
  const [newRoom, setNewRoom] = useState<CreateHotelRoomPayload>({
    roomType: 'STANDARD',
    roomName: '',
    capacity: 2,
    quantityTotal: 1,
    quantityAvailable: 1,
    status: 'ACTIVE',
  })
  const [editingRoomId, setEditingRoomId] = useState<string | null>(null)
  const [editRoomForm, setEditRoomForm] = useState<UpdateHotelRoomPayload>({})
  const roomFileInputRefs = useRef<Record<string, HTMLInputElement | null>>({})
  const fileInputRef = useRef<HTMLInputElement>(null)

  const run = useCallback(
    async (fn: () => Promise<unknown>): Promise<void> => {
      setErr(null)
      setBusy(true)
      try {
        await fn()
        await onRefresh()
      } catch (e) {
        setErr(getApiErrorMessage(e, 'Action failed'))
        throw e
      } finally {
        setBusy(false)
      }
    },
    [onRefresh],
  )

  const startEditImage = (img: ServiceImageDto) => {
    setEditingImageId(img.id)
    setEditImageForm({
      url: img.url,
      altText: img.altText ?? '',
      category: img.category,
      contextKey: img.contextKey ?? '',
      primary: img.primary,
      displayOrder: img.displayOrder,
    })
  }

  const saveImageEdit = () => {
    if (!editingImageId) return
    const id = editingImageId
    void run(() =>
      api.updateImage(serviceId, id, {
        ...editImageForm,
        altText: editImageForm.altText === '' ? undefined : editImageForm.altText,
        contextKey: editImageForm.contextKey === '' ? undefined : editImageForm.contextKey,
      }),
    )
      .then(() => setEditingImageId(null))
      .catch(() => {})
  }

  const deleteImage = (imageId: string) => {
    if (!window.confirm('Remove this image?')) return
    void run(() => api.deleteImage(serviceId, imageId))
  }

  const uploadImageFile = () => {
    const input = fileInputRef.current
    const file = input?.files?.[0]
    if (!file) {
      setErr('Choose an image file (JPEG, PNG, WebP, or GIF, max 10MB)')
      return
    }
    const fd = new FormData()
    fd.append('file', file)
    if (newImage.altText?.trim()) fd.append('altText', newImage.altText.trim())
    fd.append('category', newImage.category ?? 'PROPERTY')
    if (newImage.contextKey?.trim()) fd.append('contextKey', newImage.contextKey.trim())
    if (newImage.primary) fd.append('primary', 'true')
    void run(() => api.uploadImage(serviceId, fd))
      .then(() => {
        if (input) input.value = ''
      })
      .catch(() => {})
  }

  const addImage = () => {
    if (!newImage.url?.trim()) {
      setErr('Image URL is required')
      return
    }
    void run(() =>
      api.addImage(serviceId, {
        url: newImage.url.trim(),
        altText: newImage.altText?.trim() || undefined,
        category: newImage.category,
        contextKey: newImage.contextKey?.trim() || undefined,
        primary: newImage.primary ?? false,
      }),
    )
      .then(() =>
        setNewImage({
          url: '',
          altText: '',
          category: 'PROPERTY',
          contextKey: '',
          primary: false,
        }),
      )
      .catch(() => {})
  }

  const addSection = () => {
    const t = newSectionTitle.trim()
    if (!t) {
      setErr('Section title is required')
      return
    }
    void run(() => api.createMenuSection(serviceId, { title: t }))
      .then(() => setNewSectionTitle(''))
      .catch(() => {})
  }

  const deleteSection = (sectionId: string) => {
    if (!window.confirm('Delete this section and all its items?')) return
    void run(() => api.deleteMenuSection(serviceId, sectionId))
  }

  const getNewItem = (sectionId: string): CreateMenuItemPayload =>
    newItemBySection[sectionId] ?? {
      name: '',
      description: '',
      price: undefined,
      currency: '',
      imageUrl: '',
    }

  const setNewItem = (sectionId: string, patch: Partial<CreateMenuItemPayload>) => {
    setNewItemBySection((prev) => ({
      ...prev,
      [sectionId]: { ...getNewItem(sectionId), ...patch },
    }))
  }

  const addItem = (sectionId: string) => {
    const body = getNewItem(sectionId)
    if (!body.name?.trim()) {
      setErr('Item name is required')
      return
    }
    void run(() =>
      api.createMenuItem(serviceId, sectionId, {
        name: body.name.trim(),
        description: body.description?.trim() || undefined,
        price: body.price,
        currency: body.currency?.trim() || undefined,
        imageUrl: body.imageUrl?.trim() || undefined,
      }),
    )
      .then(() => setNewItem(sectionId, { name: '', description: '', price: undefined, currency: '', imageUrl: '' }))
      .catch(() => {})
  }

  const startEditItem = (item: RestaurantMenuItemDto) => {
    setEditingItemId(item.id)
    setEditItemForm({
      name: item.name,
      description: item.description ?? '',
      price: item.price,
      currency: item.currency ?? '',
      imageUrl: item.imageUrl ?? '',
    })
  }

  const saveItemEdit = () => {
    if (!editingItemId) return
    const id = editingItemId
    void run(() =>
      api.updateMenuItem(serviceId, id, {
        name: editItemForm.name?.trim(),
        description: editItemForm.description,
        price: editItemForm.price,
        currency: editItemForm.currency?.trim() || undefined,
        imageUrl: editItemForm.imageUrl?.trim() || undefined,
      }),
    )
      .then(() => setEditingItemId(null))
      .catch(() => {})
  }

  const uploadItemImage = (itemId: string, file: File | null) => {
    if (!file) {
      setErr('Choose an image file first')
      return
    }
    const fd = new FormData()
    fd.append('file', file)
    void run(() => api.uploadMenuItemImage(serviceId, itemId, fd)).catch(() => {})
  }

  const deleteItem = (itemId: string) => {
    if (!window.confirm('Remove this menu item?')) return
    void run(() => api.deleteMenuItem(serviceId, itemId))
  }

  const addRoom = () => {
    if (!newRoom.roomName?.trim()) {
      setErr('Room name is required')
      return
    }
    void run(async () => {
      await api.createRoom(serviceId, {
        ...newRoom,
        roomName: newRoom.roomName.trim(),
        roomType: (newRoom.roomType || 'STANDARD').trim(),
      })
      await refreshRooms()
    })
      .then(() =>
        setNewRoom({
          roomType: 'STANDARD',
          roomName: '',
          capacity: 2,
          quantityTotal: 1,
          quantityAvailable: 1,
          status: 'ACTIVE',
        }),
      )
      .catch(() => {})
  }

  const startEditRoom = (room: HotelRoomDto) => {
    setEditingRoomId(room.id)
    setEditRoomForm({
      roomType: room.roomType,
      roomName: room.roomName,
      description: room.description,
      capacity: room.capacity,
      basePrice: room.basePrice,
      currency: room.currency,
      quantityTotal: room.quantityTotal,
      quantityAvailable: room.quantityAvailable,
      status: room.status,
      sortOrder: room.sortOrder,
    })
  }

  const saveRoomEdit = () => {
    if (!editingRoomId) return
    const roomId = editingRoomId
    void run(async () => {
      await api.updateRoom(serviceId, roomId, editRoomForm)
      await refreshRooms()
    })
      .then(() => setEditingRoomId(null))
      .catch(() => {})
  }

  const deleteRoom = (roomId: string) => {
    if (!window.confirm('Delete this room type?')) return
    void run(async () => {
      await api.deleteRoom(serviceId, roomId)
      await refreshRooms()
    }).catch(() => {})
  }

  const uploadRoomImage = (roomId: string) => {
    const input = roomFileInputRefs.current[roomId]
    const file = input?.files?.[0]
    if (!file) {
      setErr('Choose an image file first')
      return
    }
    const fd = new FormData()
    fd.append('file', file)
    void run(async () => {
      await api.uploadRoomImage(serviceId, roomId, fd)
      await refreshRooms()
      if (input) input.value = ''
    }).catch(() => {})
  }

  const sections = menu?.sections ?? []

  const refreshRooms = useCallback(async () => {
    if (serviceType !== 'HOTEL') return
    const res = await api.listRooms(serviceId)
    setRooms(Array.isArray(res.data) ? res.data : [])
  }, [api, serviceId, serviceType])

  useEffect(() => {
    if (open && serviceType === 'HOTEL') {
      void refreshRooms().catch(() => {})
    }
  }, [open, refreshRooms, serviceType])

  return (
    <div className="mt-6">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="text-sm font-medium text-primary hover:underline"
      >
        {open ? '▼ Hide' : '▶ Manage photos & menu'}
      </button>
      {open && (
        <Card className="mt-3 space-y-8 p-5">
          {err && (
            <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
              {err}
            </p>
          )}

          <section>
            <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">Images (URL)</h3>
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
              Paste a public image URL (CDN or HTTPS). Max 100 images per service.
            </p>

            {images.length > 0 && (
              <ul className="mt-4 space-y-3">
                {images.map((img) => (
                  <li
                    key={img.id}
                    className="rounded-lg border border-slate-200 p-3 dark:border-slate-700"
                  >
                    {editingImageId === img.id ? (
                      <div className="grid gap-2 sm:grid-cols-2">
                        <div className="sm:col-span-2">
                          <label className={labelClass} htmlFor={`img-url-${img.id}`}>
                            URL
                          </label>
                          <input
                            id={`img-url-${img.id}`}
                            className={inputClass}
                            value={editImageForm.url ?? ''}
                            onChange={(e) => setEditImageForm((f) => ({ ...f, url: e.target.value }))}
                          />
                        </div>
                        <div>
                          <label className={labelClass} htmlFor={`img-alt-${img.id}`}>
                            Alt text
                          </label>
                          <input
                            id={`img-alt-${img.id}`}
                            className={inputClass}
                            value={editImageForm.altText ?? ''}
                            onChange={(e) => setEditImageForm((f) => ({ ...f, altText: e.target.value }))}
                          />
                        </div>
                        <div>
                          <label className={labelClass} htmlFor={`img-cat-${img.id}`}>
                            Category
                          </label>
                          <select
                            id={`img-cat-${img.id}`}
                            className={inputClass}
                            value={editImageForm.category ?? 'PROPERTY'}
                            onChange={(e) =>
                              setEditImageForm((f) => ({
                                ...f,
                                category: e.target.value as ServiceImageCategoryDto,
                              }))
                            }
                          >
                            {IMAGE_CATEGORIES.map((c) => (
                              <option key={c} value={c}>
                                {c}
                              </option>
                            ))}
                          </select>
                        </div>
                        <div>
                          <label className={labelClass} htmlFor={`img-ctx-${img.id}`}>
                            Context / room label
                          </label>
                          <input
                            id={`img-ctx-${img.id}`}
                            className={inputClass}
                            value={editImageForm.contextKey ?? ''}
                            onChange={(e) => setEditImageForm((f) => ({ ...f, contextKey: e.target.value }))}
                            placeholder="e.g. Deluxe twin"
                          />
                        </div>
                        <div>
                          <label className={labelClass} htmlFor={`img-ord-${img.id}`}>
                            Display order
                          </label>
                          <input
                            id={`img-ord-${img.id}`}
                            type="number"
                            className={inputClass}
                            value={editImageForm.displayOrder ?? 0}
                            onChange={(e) =>
                              setEditImageForm((f) => ({
                                ...f,
                                displayOrder: Number.parseInt(e.target.value, 10) || 0,
                              }))
                            }
                          />
                        </div>
                        <div className="flex items-end gap-2">
                          <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
                            <input
                              type="checkbox"
                              checked={!!editImageForm.primary}
                              onChange={(e) => setEditImageForm((f) => ({ ...f, primary: e.target.checked }))}
                            />
                            Primary image
                          </label>
                        </div>
                        <div className="flex flex-wrap gap-2 sm:col-span-2">
                          <button type="button" className={btnPrimary} disabled={busy} onClick={saveImageEdit}>
                            Save
                          </button>
                          <button
                            type="button"
                            className={btnSecondary}
                            disabled={busy}
                            onClick={() => setEditingImageId(null)}
                          >
                            Cancel
                          </button>
                        </div>
                      </div>
                    ) : (
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <div className="min-w-0">
                          <p className="truncate text-sm font-medium text-slate-900 dark:text-slate-100">{img.url}</p>
                          <p className="text-xs text-slate-500 dark:text-slate-400">
                            {img.category}
                            {img.contextKey ? ` · ${img.contextKey}` : ''}
                            {img.primary ? ' · primary' : ''}
                          </p>
                        </div>
                        <div className="flex gap-2">
                          <button type="button" className={btnSecondary} disabled={busy} onClick={() => startEditImage(img)}>
                            Edit
                          </button>
                          <button type="button" className={btnDanger} disabled={busy} onClick={() => deleteImage(img.id)}>
                            Delete
                          </button>
                        </div>
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            )}

            <div className="mt-4 rounded-lg border border-dashed border-slate-300 p-4 dark:border-slate-600">
              <p className={labelClass}>Upload file</p>
              <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                Stored on the server; same category / alt / context / primary fields below apply.
              </p>
              <div className="mt-2 flex flex-wrap items-center gap-2">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp,image/gif"
                  className="max-w-full text-sm text-slate-700 file:mr-2 file:rounded-md file:border-0 file:bg-primary file:px-2 file:py-1 file:text-sm file:text-white dark:text-slate-300"
                />
                <button type="button" className={btnPrimary} disabled={busy} onClick={uploadImageFile}>
                  Upload
                </button>
              </div>
              <p className={`${labelClass} mt-4`}>Or paste URL</p>
              <div className="mt-2 grid gap-2 sm:grid-cols-2">
                <div className="sm:col-span-2">
                  <input
                    className={inputClass}
                    placeholder="https://…"
                    value={newImage.url}
                    onChange={(e) => setNewImage((n) => ({ ...n, url: e.target.value }))}
                  />
                </div>
                <div>
                  <input
                    className={inputClass}
                    placeholder="Alt text (optional)"
                    value={newImage.altText}
                    onChange={(e) => setNewImage((n) => ({ ...n, altText: e.target.value }))}
                  />
                </div>
                <div>
                  <select
                    className={inputClass}
                    value={newImage.category}
                    onChange={(e) =>
                      setNewImage((n) => ({ ...n, category: e.target.value as ServiceImageCategoryDto }))
                    }
                  >
                    {IMAGE_CATEGORIES.map((c) => (
                      <option key={c} value={c}>
                        {c}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <input
                    className={inputClass}
                    placeholder="Context / room label (optional)"
                    value={newImage.contextKey}
                    onChange={(e) => setNewImage((n) => ({ ...n, contextKey: e.target.value }))}
                  />
                </div>
                <div className="flex items-center gap-2">
                  <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
                    <input
                      type="checkbox"
                      checked={!!newImage.primary}
                      onChange={(e) => setNewImage((n) => ({ ...n, primary: e.target.checked }))}
                    />
                    Set as primary
                  </label>
                </div>
                <div className="sm:col-span-2">
                  <button type="button" className={btnPrimary} disabled={busy} onClick={addImage}>
                    Add image
                  </button>
                </div>
              </div>
            </div>
          </section>

          {serviceType === 'RESTAURANT' && (
            <section>
              <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">Restaurant menu</h3>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                Sections and dishes. Currency on each item is optional (defaults to service currency in the public view).
              </p>

              <div className="mt-4 flex flex-wrap gap-2">
                <input
                  className={`${inputClass} max-w-md`}
                  placeholder="New section title"
                  value={newSectionTitle}
                  onChange={(e) => setNewSectionTitle(e.target.value)}
                />
                <button type="button" className={btnPrimary} disabled={busy} onClick={addSection}>
                  Add section
                </button>
              </div>

              <div className="mt-6 space-y-6">
                {sections.map((sec) => (
                  <div
                    key={sec.id}
                    className="rounded-lg border border-slate-200 p-4 dark:border-slate-700"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <h4 className="font-medium text-slate-900 dark:text-slate-100">{sec.title}</h4>
                      <button
                        type="button"
                        className={btnDanger}
                        disabled={busy}
                        onClick={() => deleteSection(sec.id)}
                      >
                        Delete section
                      </button>
                    </div>

                    <ul className="mt-3 space-y-3">
                      {sec.items.map((item) => (
                        <li
                          key={item.id}
                          className="rounded-md border border-slate-100 bg-slate-50/80 p-3 dark:border-slate-800 dark:bg-slate-900/40"
                        >
                          {editingItemId === item.id ? (
                            <div className="grid gap-2 sm:grid-cols-2">
                              <div>
                                <label className={labelClass}>Name</label>
                                <input
                                  className={inputClass}
                                  value={editItemForm.name ?? ''}
                                  onChange={(e) => setEditItemForm((f) => ({ ...f, name: e.target.value }))}
                                />
                              </div>
                              <div>
                                <label className={labelClass}>Price</label>
                                <input
                                  type="number"
                                  step="0.01"
                                  className={inputClass}
                                  value={editItemForm.price ?? ''}
                                  onChange={(e) =>
                                    setEditItemForm((f) => ({
                                      ...f,
                                      price: e.target.value === '' ? undefined : Number.parseFloat(e.target.value),
                                    }))
                                  }
                                />
                              </div>
                              <div>
                                <label className={labelClass}>Currency</label>
                                <input
                                  className={inputClass}
                                  placeholder={currency}
                                  value={editItemForm.currency ?? ''}
                                  onChange={(e) => setEditItemForm((f) => ({ ...f, currency: e.target.value }))}
                                />
                              </div>
                              <div className="sm:col-span-2">
                                <label className={labelClass}>Image URL</label>
                                <input
                                  className={inputClass}
                                  value={editItemForm.imageUrl ?? ''}
                                  onChange={(e) => setEditItemForm((f) => ({ ...f, imageUrl: e.target.value }))}
                                />
                              </div>
                              <div className="sm:col-span-2">
                                <label className={labelClass}>Description</label>
                                <textarea
                                  className={inputClass}
                                  rows={2}
                                  value={editItemForm.description ?? ''}
                                  onChange={(e) => setEditItemForm((f) => ({ ...f, description: e.target.value }))}
                                />
                              </div>
                              <div className="flex gap-2 sm:col-span-2">
                                <button type="button" className={btnPrimary} disabled={busy} onClick={saveItemEdit}>
                                  Save item
                                </button>
                                <button
                                  type="button"
                                  className={btnSecondary}
                                  disabled={busy}
                                  onClick={() => setEditingItemId(null)}
                                >
                                  Cancel
                                </button>
                              </div>
                            </div>
                          ) : (
                            <div className="flex flex-wrap items-start justify-between gap-2">
                              <div>
                                <span className="font-medium text-slate-900 dark:text-slate-100">{item.name}</span>
                                {item.price != null && (
                                  <span className="ml-2 text-sm text-slate-600 dark:text-slate-400">
                                    {item.currency ?? currency} {item.price}
                                  </span>
                                )}
                                {item.description && (
                                  <p className="mt-1 text-xs text-slate-600 dark:text-slate-400">{item.description}</p>
                                )}
                              </div>
                              <div className="flex gap-2">
                                <label className={btnSecondary}>
                                  Upload image
                                  <input
                                    type="file"
                                    accept="image/jpeg,image/png,image/webp,image/gif"
                                    className="hidden"
                                    onChange={(e) => uploadItemImage(item.id, e.target.files?.[0] ?? null)}
                                  />
                                </label>
                                <button
                                  type="button"
                                  className={btnSecondary}
                                  disabled={busy}
                                  onClick={() => startEditItem(item)}
                                >
                                  Edit
                                </button>
                                <button
                                  type="button"
                                  className={btnDanger}
                                  disabled={busy}
                                  onClick={() => deleteItem(item.id)}
                                >
                                  Delete
                                </button>
                              </div>
                            </div>
                          )}
                        </li>
                      ))}
                    </ul>

                    <div className="mt-3 rounded border border-dashed border-slate-300 p-3 dark:border-slate-600">
                      <p className="text-xs font-medium text-slate-600 dark:text-slate-400">New item in this section</p>
                      <div className="mt-2 grid gap-2 sm:grid-cols-2">
                        <input
                          className={inputClass}
                          placeholder="Name"
                          value={getNewItem(sec.id).name}
                          onChange={(e) => setNewItem(sec.id, { name: e.target.value })}
                        />
                        <input
                          type="number"
                          step="0.01"
                          className={inputClass}
                          placeholder="Price"
                          value={getNewItem(sec.id).price ?? ''}
                          onChange={(e) =>
                            setNewItem(sec.id, {
                              price: e.target.value === '' ? undefined : Number.parseFloat(e.target.value),
                            })
                          }
                        />
                        <input
                          className={inputClass}
                          placeholder={`Currency (default ${currency})`}
                          value={getNewItem(sec.id).currency ?? ''}
                          onChange={(e) => setNewItem(sec.id, { currency: e.target.value })}
                        />
                        <input
                          className={inputClass}
                          placeholder="Image URL (optional)"
                          value={getNewItem(sec.id).imageUrl ?? ''}
                          onChange={(e) => setNewItem(sec.id, { imageUrl: e.target.value })}
                        />
                        <textarea
                          className={`${inputClass} sm:col-span-2`}
                          rows={2}
                          placeholder="Description (optional)"
                          value={getNewItem(sec.id).description ?? ''}
                          onChange={(e) => setNewItem(sec.id, { description: e.target.value })}
                        />
                        <button type="button" className={btnPrimary} disabled={busy} onClick={() => addItem(sec.id)}>
                          Add item
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {serviceType === 'HOTEL' && (
            <section>
              <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">Hotel rooms</h3>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                Manage room types, inventory, and room photos.
              </p>

              <div className="mt-4 rounded border border-dashed border-slate-300 p-3 dark:border-slate-600">
                <p className="text-xs font-medium text-slate-600 dark:text-slate-400">Add room type</p>
                <div className="mt-2 grid gap-2 sm:grid-cols-3">
                  <input className={inputClass} placeholder="Room type (e.g. DELUXE)" value={newRoom.roomType}
                    onChange={(e) => setNewRoom((r) => ({ ...r, roomType: e.target.value }))} />
                  <input className={inputClass} placeholder="Room name" value={newRoom.roomName}
                    onChange={(e) => setNewRoom((r) => ({ ...r, roomName: e.target.value }))} />
                  <input type="number" className={inputClass} placeholder="Capacity" value={newRoom.capacity ?? 2}
                    onChange={(e) => setNewRoom((r) => ({ ...r, capacity: Number.parseInt(e.target.value, 10) || 1 }))} />
                  <input type="number" className={inputClass} placeholder="Total quantity" value={newRoom.quantityTotal ?? 0}
                    onChange={(e) => setNewRoom((r) => ({ ...r, quantityTotal: Number.parseInt(e.target.value, 10) || 0 }))} />
                  <input type="number" className={inputClass} placeholder="Available quantity" value={newRoom.quantityAvailable ?? 0}
                    onChange={(e) => setNewRoom((r) => ({ ...r, quantityAvailable: Number.parseInt(e.target.value, 10) || 0 }))} />
                  <input type="number" step="0.01" className={inputClass} placeholder="Base price"
                    value={newRoom.basePrice ?? ''}
                    onChange={(e) => setNewRoom((r) => ({ ...r, basePrice: e.target.value === '' ? undefined : Number.parseFloat(e.target.value) }))} />
                </div>
                <button type="button" className={`${btnPrimary} mt-2`} disabled={busy} onClick={addRoom}>Add room</button>
              </div>

              <div className="mt-4 space-y-3">
                {rooms.map((room) => (
                  <div key={room.id} className="rounded-lg border border-slate-200 p-3 dark:border-slate-700">
                    {editingRoomId === room.id ? (
                      <div className="grid gap-2 sm:grid-cols-3">
                        <input className={inputClass} value={editRoomForm.roomType ?? ''} onChange={(e) => setEditRoomForm((f) => ({ ...f, roomType: e.target.value }))} />
                        <input className={inputClass} value={editRoomForm.roomName ?? ''} onChange={(e) => setEditRoomForm((f) => ({ ...f, roomName: e.target.value }))} />
                        <input type="number" className={inputClass} value={editRoomForm.capacity ?? 1} onChange={(e) => setEditRoomForm((f) => ({ ...f, capacity: Number.parseInt(e.target.value, 10) || 1 }))} />
                        <input type="number" className={inputClass} value={editRoomForm.quantityTotal ?? 0} onChange={(e) => setEditRoomForm((f) => ({ ...f, quantityTotal: Number.parseInt(e.target.value, 10) || 0 }))} />
                        <input type="number" className={inputClass} value={editRoomForm.quantityAvailable ?? 0} onChange={(e) => setEditRoomForm((f) => ({ ...f, quantityAvailable: Number.parseInt(e.target.value, 10) || 0 }))} />
                        <div className="flex gap-2">
                          <button type="button" className={btnPrimary} onClick={saveRoomEdit} disabled={busy}>Save</button>
                          <button type="button" className={btnSecondary} onClick={() => setEditingRoomId(null)} disabled={busy}>Cancel</button>
                        </div>
                      </div>
                    ) : (
                      <div>
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <p className="text-sm font-medium text-slate-900 dark:text-slate-100">
                            {room.roomName} ({room.roomType}) - {room.quantityAvailable}/{room.quantityTotal}
                          </p>
                          <div className="flex gap-2">
                            <button type="button" className={btnSecondary} onClick={() => startEditRoom(room)} disabled={busy}>Edit</button>
                            <button type="button" className={btnDanger} onClick={() => deleteRoom(room.id)} disabled={busy}>Delete</button>
                          </div>
                        </div>
                        <div className="mt-2 flex flex-wrap items-center gap-2">
                          <input
                            ref={(el) => {
                              roomFileInputRefs.current[room.id] = el
                            }}
                            type="file"
                            accept="image/jpeg,image/png,image/webp,image/gif"
                            className="text-sm"
                          />
                          <button type="button" className={btnSecondary} onClick={() => uploadRoomImage(room.id)} disabled={busy}>
                            Upload room image
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </section>
          )}
        </Card>
      )}
    </div>
  )
}
