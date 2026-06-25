import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, portalAPI, portalServicesAPI, portalWalkInAPI } from '../../services/api'
import type { HotelRoomDto, CreateHotelRoomPayload, UpdateHotelRoomPayload, PageDto, ServiceDto } from '../../types/api'
import { Card } from '../../components/Card'
import { Modal } from '../../components/Modal'
import { ConfirmDialog } from '../../components/ConfirmDialog'

const ROOM_STATUSES = ['ACTIVE', 'INACTIVE'] as const
const ROOM_CATEGORIES = ['STANDARD', 'DELUXE', 'SUITE', 'EXECUTIVE'] as const
const BED_TYPES = ['SINGLE', 'DOUBLE', 'TWIN', 'KING', 'QUEEN'] as const
const VIEW_TYPES = ['NONE', 'CITY', 'GARDEN', 'POOL', 'SEA'] as const
const AMENITY_OPTIONS = ['wifi', 'ac', 'tv', 'minibar', 'safe', 'balcony', 'kitchen', 'breakfast'] as const

const INPUT_CLS =
  'w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800'

function RoomForm({
  form,
  setForm,
  formError,
}: {
  form: CreateHotelRoomPayload
  setForm: React.Dispatch<React.SetStateAction<CreateHotelRoomPayload>>
  formError: string
}) {
  const { t } = useLanguage()

  function toggleAmenity(key: string) {
    const current = (form.amenities ?? {}) as Record<string, boolean>
    setForm((f) => ({ ...f, amenities: { ...current, [key]: !current[key] } }))
  }

  const amenities = (form.amenities ?? {}) as Record<string, boolean>

  return (
    <div className="space-y-3">
      {formError && <p className="text-sm text-red-500">{formError}</p>}

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldName')}</label>
          <input className={INPUT_CLS} value={form.roomName}
            onChange={(e) => setForm((f) => ({ ...f, roomName: e.target.value }))} />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldType')}</label>
          <input className={INPUT_CLS} placeholder="e.g. Deluxe King" value={form.roomType}
            onChange={(e) => setForm((f) => ({ ...f, roomType: e.target.value }))} />
        </div>
      </div>

      <div>
        <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldDescription')}</label>
        <textarea className={INPUT_CLS} rows={2} value={form.description ?? ''}
          onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} />
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldCategory')}</label>
          <select className={INPUT_CLS} value={form.roomCategory ?? 'STANDARD'}
            onChange={(e) => setForm((f) => ({ ...f, roomCategory: e.target.value }))}>
            {ROOM_CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldBedType')}</label>
          <select className={INPUT_CLS} value={form.bedType ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, bedType: e.target.value || undefined }))}>
            <option value="">—</option>
            {BED_TYPES.map((b) => <option key={b} value={b}>{b}</option>)}
          </select>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-3">
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldCapacity')}</label>
          <input type="number" min={1} className={INPUT_CLS} value={form.capacity}
            onChange={(e) => setForm((f) => ({ ...f, capacity: Number(e.target.value) }))} />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldAreaSqm')}</label>
          <input type="number" min={0} step="0.1" className={INPUT_CLS} value={form.areaSqm ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, areaSqm: e.target.value ? Number(e.target.value) : undefined }))} />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldViewType')}</label>
          <select className={INPUT_CLS} value={form.viewType ?? 'NONE'}
            onChange={(e) => setForm((f) => ({ ...f, viewType: e.target.value }))}>
            {VIEW_TYPES.map((v) => <option key={v} value={v}>{v}</option>)}
          </select>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldBasePrice')}</label>
          <input type="number" min={0} step="0.01" className={INPUT_CLS} value={form.basePrice ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, basePrice: e.target.value ? Number(e.target.value) : undefined }))} />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">Currency</label>
          <input className={INPUT_CLS} maxLength={3} value={form.currency ?? 'USD'}
            onChange={(e) => setForm((f) => ({ ...f, currency: e.target.value }))} />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldQuantityTotal')}</label>
          <input type="number" min={1} className={INPUT_CLS} value={form.quantityTotal}
            onChange={(e) => setForm((f) => ({ ...f, quantityTotal: Number(e.target.value) }))} />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldQuantityAvailable')}</label>
          <input type="number" min={0} className={INPUT_CLS} value={form.quantityAvailable}
            onChange={(e) => setForm((f) => ({ ...f, quantityAvailable: Number(e.target.value) }))} />
        </div>
      </div>

      <div className="grid grid-cols-3 gap-3">
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldFloor')}</label>
          <input type="number" className={INPUT_CLS} value={form.floorNumber ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, floorNumber: e.target.value ? Number(e.target.value) : undefined }))} />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldSortOrder')}</label>
          <input type="number" min={0} className={INPUT_CLS} value={form.sortOrder ?? 0}
            onChange={(e) => setForm((f) => ({ ...f, sortOrder: Number(e.target.value) }))} />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldStatus')}</label>
          <select className={INPUT_CLS} value={form.status ?? 'ACTIVE'}
            onChange={(e) => setForm((f) => ({ ...f, status: e.target.value as 'ACTIVE' | 'INACTIVE' }))}>
            {ROOM_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </div>
      </div>

      <div className="flex gap-6">
        <label className="flex items-center gap-2 text-sm cursor-pointer">
          <input type="checkbox" checked={!!form.smokingAllowed}
            onChange={(e) => setForm((f) => ({ ...f, smokingAllowed: e.target.checked }))} />
          {t('portalPages.roomFieldSmokingAllowed')}
        </label>
        <label className="flex items-center gap-2 text-sm cursor-pointer">
          <input type="checkbox" checked={!!form.isAccessible}
            onChange={(e) => setForm((f) => ({ ...f, isAccessible: e.target.checked }))} />
          {t('portalPages.roomFieldIsAccessible')}
        </label>
      </div>

      <div>
        <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldAmenities')}</label>
        <div className="flex flex-wrap gap-2">
          {AMENITY_OPTIONS.map((key) => (
            <label key={key} className={`flex items-center gap-1.5 rounded-lg border px-2.5 py-1.5 text-xs cursor-pointer transition-colors ${
              amenities[key]
                ? 'border-primary bg-primary/10 text-primary'
                : 'border-slate-200 text-slate-500 dark:border-slate-700'
            }`}>
              <input type="checkbox" className="sr-only" checked={!!amenities[key]} onChange={() => toggleAmenity(key)} />
              {key.charAt(0).toUpperCase() + key.slice(1)}
            </label>
          ))}
        </div>
      </div>
    </div>
  )
}

function WalkInModal({
  open,
  onClose,
  room,
  serviceId,
  onSuccess,
}: {
  open: boolean
  onClose: () => void
  room: HotelRoomDto | null
  serviceId: string
  onSuccess: (msg: string) => void
}) {
  const { t } = useLanguage()
  const [checkIn, setCheckIn] = useState('')
  const [checkOut, setCheckOut] = useState('')
  const [reason, setReason] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (open) {
      setCheckIn('')
      setCheckOut('')
      setReason('')
      setError('')
    }
  }, [open])

  async function handleSubmit() {
    if (!room || !checkIn || !checkOut) return
    setSaving(true)
    setError('')
    try {
      const res = await portalWalkInAPI.markOccupied(serviceId, room.id, {
        checkInDate: checkIn,
        checkOutDate: checkOut,
        reason: reason || undefined,
      })
      const data = res.data as { cancelledBookingIds?: string[] }
      const count = data?.cancelledBookingIds?.length ?? 0
      onSuccess(t('portalPages.walkInSuccess').replace('{count}', String(count)))
      onClose()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={t('portalPages.walkInTitle')}>
      <p className="mb-4 text-sm text-slate-500">{t('portalPages.walkInDescription')}</p>
      {room && <p className="mb-3 text-sm font-medium">{room.roomName} ({room.roomType})</p>}
      {error && <p className="mb-2 text-sm text-red-500">{error}</p>}
      <div className="space-y-3">
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.walkInCheckIn')}</label>
          <input type="date" className={INPUT_CLS} value={checkIn} onChange={(e) => setCheckIn(e.target.value)} />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.walkInCheckOut')}</label>
          <input type="date" className={INPUT_CLS} value={checkOut} onChange={(e) => setCheckOut(e.target.value)} />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.walkInReason')}</label>
          <input className={INPUT_CLS} value={reason} onChange={(e) => setReason(e.target.value)} />
        </div>
      </div>
      <div className="mt-4 flex justify-end gap-2">
        <button onClick={onClose} className="rounded-xl border border-slate-300 px-4 py-2 text-sm">{t('ui.cancel')}</button>
        <button onClick={handleSubmit} disabled={saving || !checkIn || !checkOut}
          className="dashboard-btn-primary disabled:opacity-50">
          {saving ? t('portalPages.saving') : t('portalPages.walkInSubmit')}
        </button>
      </div>
    </Modal>
  )
}

function RoomCard({ room, onEdit, onDelete, onWalkIn }: {
  room: HotelRoomDto
  onEdit: () => void
  onDelete: () => void
  onWalkIn: () => void
}) {
  return (
    <Card className="!p-4 flex flex-col gap-2">
      <div className="flex items-start justify-between">
        <div>
          <h3 className="font-medium text-sm">{room.roomName}</h3>
          <p className="text-xs text-slate-400">{room.roomType}</p>
        </div>
        <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
          room.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-slate-100 text-slate-500'
        }`}>{room.status}</span>
      </div>

      <div className="flex flex-wrap gap-1.5">
        {room.roomCategory && room.roomCategory !== 'STANDARD' && (
          <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600 dark:bg-slate-700 dark:text-slate-300">{room.roomCategory}</span>
        )}
        {room.bedType && (
          <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600 dark:bg-slate-700 dark:text-slate-300">{room.bedType}</span>
        )}
        {room.viewType && room.viewType !== 'NONE' && (
          <span className="rounded-full bg-blue-50 px-2 py-0.5 text-xs text-blue-600 dark:bg-blue-900/30 dark:text-blue-300">{room.viewType} view</span>
        )}
        {room.isAccessible && (
          <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-xs text-emerald-600 dark:bg-emerald-900/30">♿ Accessible</span>
        )}
        {room.smokingAllowed && (
          <span className="rounded-full bg-amber-50 px-2 py-0.5 text-xs text-amber-600 dark:bg-amber-900/30">Smoking</span>
        )}
      </div>

      <div className="flex gap-4 text-xs text-slate-500">
        {room.floorNumber != null && <span>Floor {room.floorNumber}</span>}
        <span>Cap: {room.capacity}</span>
        {room.areaSqm != null && <span>{room.areaSqm} m²</span>}
        <span>{room.quantityAvailable}/{room.quantityTotal} avail</span>
        {room.basePrice != null && <span>{room.basePrice} {room.currency ?? ''}</span>}
      </div>

      <div className="mt-auto flex gap-2 pt-2 border-t border-slate-100 dark:border-slate-700">
        <button onClick={onEdit} className="text-xs text-primary hover:underline">Edit</button>
        <button onClick={onWalkIn} className="text-xs text-amber-600 hover:underline">Walk-in</button>
        <button onClick={onDelete} className="text-xs text-red-500 hover:underline">Delete</button>
      </div>
    </Card>
  )
}

export function PortalRoomsPage() {
  const { t } = useLanguage()

  const [serviceId, setServiceId] = useState<string | null>(null)
  const [rooms, setRooms] = useState<HotelRoomDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const [addOpen, setAddOpen] = useState(false)
  const [editRoom, setEditRoom] = useState<HotelRoomDto | null>(null)
  const [deleteRoom, setDeleteRoom] = useState<HotelRoomDto | null>(null)
  const [walkInRoom, setWalkInRoom] = useState<HotelRoomDto | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid')

  const [floors, setFloors] = useState<number[]>([])
  const [filterFloor, setFilterFloor] = useState<string>('')
  const [filterCategory, setFilterCategory] = useState<string>('')
  const [filterStatus, setFilterStatus] = useState<string>('')

  const emptyForm = (): CreateHotelRoomPayload => ({
    roomType: '',
    roomName: '',
    description: '',
    capacity: 1,
    basePrice: undefined,
    currency: 'USD',
    quantityTotal: 1,
    quantityAvailable: 1,
    status: 'ACTIVE',
    floorNumber: undefined,
    roomCategory: 'STANDARD',
    bedType: undefined,
    areaSqm: undefined,
    viewType: 'NONE',
    smokingAllowed: false,
    isAccessible: false,
    sortOrder: 0,
    amenities: {},
  })

  const [form, setForm] = useState<CreateHotelRoomPayload>(emptyForm())

  useEffect(() => {
    portalAPI.listServices({ page: 0, size: 1 })
      .then((res) => {
        const page = res.data as PageDto<ServiceDto> | null
        const svc = page?.content?.[0]
        if (!svc) {
          setError(t('portalPages.noServiceFound'))
          setLoading(false)
          return
        }
        setServiceId(svc.id)
        return Promise.all([
          portalServicesAPI.listRooms(svc.id),
          portalWalkInAPI.getFloors(svc.id).catch(() => ({ data: [] })),
        ]).then(([roomsRes, floorsRes]) => {
          setRooms(roomsRes.data)
          setFloors((floorsRes.data as number[]) ?? [])
        })
      })
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  const filteredRooms = rooms.filter((r) => {
    if (filterFloor && r.floorNumber !== Number(filterFloor)) return false
    if (filterCategory && r.roomCategory !== filterCategory) return false
    if (filterStatus && r.status !== filterStatus) return false
    return true
  })

  function openAdd() {
    setForm(emptyForm())
    setFormError('')
    setAddOpen(true)
  }

  function openEdit(room: HotelRoomDto) {
    setForm({
      roomType: room.roomType,
      roomName: room.roomName,
      description: room.description ?? '',
      capacity: room.capacity,
      basePrice: room.basePrice,
      currency: room.currency ?? 'USD',
      quantityTotal: room.quantityTotal,
      quantityAvailable: room.quantityAvailable,
      status: room.status,
      floorNumber: room.floorNumber,
      roomCategory: room.roomCategory ?? 'STANDARD',
      bedType: room.bedType,
      areaSqm: room.areaSqm,
      viewType: room.viewType ?? 'NONE',
      smokingAllowed: room.smokingAllowed ?? false,
      isAccessible: room.isAccessible ?? false,
      sortOrder: room.sortOrder ?? 0,
      amenities: (room.amenities ?? {}) as Record<string, unknown>,
    })
    setFormError('')
    setEditRoom(room)
  }

  async function handleAdd() {
    if (!serviceId) return
    setSaving(true)
    setFormError('')
    try {
      const res = await portalServicesAPI.createRoom(serviceId, form)
      setRooms((prev) => [...prev, res.data])
      setAddOpen(false)
    } catch (e) {
      setFormError(getApiErrorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  async function handleEdit() {
    if (!serviceId || !editRoom) return
    setSaving(true)
    setFormError('')
    const patch: UpdateHotelRoomPayload = { ...form }
    try {
      const res = await portalServicesAPI.updateRoom(serviceId, editRoom.id, patch)
      setRooms((prev) => prev.map((r) => (r.id === editRoom.id ? res.data : r)))
      setEditRoom(null)
    } catch (e) {
      setFormError(getApiErrorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete() {
    if (!serviceId || !deleteRoom) return
    setSaving(true)
    try {
      await portalServicesAPI.deleteRoom(serviceId, deleteRoom.id)
      setRooms((prev) => prev.filter((r) => r.id !== deleteRoom.id))
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setSaving(false)
      setDeleteRoom(null)
    }
  }

  return (
    <>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="app-page-title">{t('portalPages.manageRooms')}</h1>
        <button onClick={openAdd} disabled={!serviceId} className="dashboard-btn-primary disabled:opacity-50">
          {t('portalPages.roomAdd')}
        </button>
      </div>

      {success && (
        <div className="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:border-emerald-800/60 dark:bg-emerald-900/20 dark:text-emerald-300">
          {success}
        </div>
      )}

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <select className={INPUT_CLS + ' !w-auto'} value={filterFloor} onChange={(e) => setFilterFloor(e.target.value)}>
          <option value="">{t('portalPages.roomAllFloors')}</option>
          {floors.map((f) => <option key={f} value={f}>Floor {f}</option>)}
        </select>
        <select className={INPUT_CLS + ' !w-auto'} value={filterCategory} onChange={(e) => setFilterCategory(e.target.value)}>
          <option value="">{t('portalPages.roomAllCategories')}</option>
          {ROOM_CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
        <select className={INPUT_CLS + ' !w-auto'} value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
          <option value="">{t('portalPages.roomAllStatuses')}</option>
          {ROOM_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
        <div className="ml-auto flex gap-1 rounded-lg bg-slate-100 p-0.5 dark:bg-white/[0.04]">
          <button onClick={() => setViewMode('grid')}
            className={`rounded-md px-3 py-1 text-xs font-medium ${viewMode === 'grid' ? 'bg-white shadow-sm dark:bg-slate-800' : 'text-slate-400'}`}>
            {t('portalPages.roomGridView')}
          </button>
          <button onClick={() => setViewMode('list')}
            className={`rounded-md px-3 py-1 text-xs font-medium ${viewMode === 'list' ? 'bg-white shadow-sm dark:bg-slate-800' : 'text-slate-400'}`}>
            {t('portalPages.roomListView')}
          </button>
        </div>
      </div>

      {error && <p className="mb-4 text-sm text-red-500">{error}</p>}
      {loading && <p className="text-sm text-slate-400">Loading…</p>}

      {!loading && filteredRooms.length === 0 && (
        <p className="text-sm text-slate-400">{t('portalPages.roomsEmpty')}</p>
      )}

      {viewMode === 'grid' && filteredRooms.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {filteredRooms.map((room) => (
            <RoomCard
              key={room.id}
              room={room}
              onEdit={() => openEdit(room)}
              onDelete={() => setDeleteRoom(room)}
              onWalkIn={() => setWalkInRoom(room)}
            />
          ))}
        </div>
      )}

      {viewMode === 'list' && filteredRooms.length > 0 && (
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-left text-xs font-medium text-slate-400 dark:border-slate-700">
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldName')}</th>
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldType')}</th>
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldCategory')}</th>
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldBedType')}</th>
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldFloor')}</th>
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldAreaSqm')}</th>
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldCapacity')}</th>
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldBasePrice')}</th>
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldQuantityAvailable')}/{t('portalPages.roomFieldQuantityTotal')}</th>
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldStatus')}</th>
                  <th className="pb-2"></th>
                </tr>
              </thead>
              <tbody>
                {filteredRooms.map((room) => (
                  <tr key={room.id} className="border-b border-slate-50 last:border-0 dark:border-slate-800">
                    <td className="py-2 pr-4 font-medium">{room.roomName}</td>
                    <td className="py-2 pr-4 text-slate-500">{room.roomType}</td>
                    <td className="py-2 pr-4">{room.roomCategory ?? 'STANDARD'}</td>
                    <td className="py-2 pr-4">{room.bedType ?? '—'}</td>
                    <td className="py-2 pr-4">{room.floorNumber ?? '—'}</td>
                    <td className="py-2 pr-4">{room.areaSqm != null ? `${room.areaSqm} m²` : '—'}</td>
                    <td className="py-2 pr-4">{room.capacity}</td>
                    <td className="py-2 pr-4">{room.basePrice != null ? `${room.basePrice} ${room.currency ?? ''}` : '—'}</td>
                    <td className="py-2 pr-4">{room.quantityAvailable}/{room.quantityTotal}</td>
                    <td className="py-2 pr-4">
                      <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                        room.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-slate-100 text-slate-500'
                      }`}>{room.status}</span>
                    </td>
                    <td className="py-2">
                      <div className="flex gap-2">
                        <button onClick={() => openEdit(room)} className="text-xs text-primary hover:underline">{t('portalPages.roomEdit')}</button>
                        <button onClick={() => setWalkInRoom(room)} className="text-xs text-amber-600 hover:underline">{t('portalPages.walkInTitle')}</button>
                        <button onClick={() => setDeleteRoom(room)} className="text-xs text-red-500 hover:underline">{t('portalPages.roomDelete')}</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <Modal open={addOpen} onClose={() => setAddOpen(false)} title={t('portalPages.roomAdd')}>
        <RoomForm form={form} setForm={setForm} formError={formError} />
        <div className="mt-4 flex justify-end gap-2">
          <button onClick={() => setAddOpen(false)}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm">{t('ui.cancel')}</button>
          <button onClick={handleAdd} disabled={saving}
            className="dashboard-btn-primary disabled:opacity-50">{saving ? t('portalPages.saving') : t('portalPages.save')}</button>
        </div>
      </Modal>

      <Modal open={!!editRoom} onClose={() => setEditRoom(null)} title={t('portalPages.roomEdit')}>
        <RoomForm form={form} setForm={setForm} formError={formError} />
        <div className="mt-4 flex justify-end gap-2">
          <button onClick={() => setEditRoom(null)}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm">{t('ui.cancel')}</button>
          <button onClick={handleEdit} disabled={saving}
            className="dashboard-btn-primary disabled:opacity-50">{saving ? t('portalPages.saving') : t('portalPages.save')}</button>
        </div>
      </Modal>

      <WalkInModal
        open={!!walkInRoom}
        onClose={() => setWalkInRoom(null)}
        room={walkInRoom}
        serviceId={serviceId ?? ''}
        onSuccess={(msg) => {
          setSuccess(msg)
          if (serviceId) portalServicesAPI.listRooms(serviceId).then((r) => setRooms(r.data))
        }}
      />

      <ConfirmDialog
        open={!!deleteRoom}
        onClose={() => setDeleteRoom(null)}
        title={t('portalPages.roomDelete')}
        description={t('portalPages.roomDeleteConfirm')}
        confirmLabel={t('portalPages.roomDelete')}
        onConfirm={handleDelete}
      />
    </>
  )
}
