import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, portalServicesAPI } from '../../services/api'
import type { HotelRoomDto, CreateHotelRoomPayload, UpdateHotelRoomPayload } from '../../types/api'
import { Card } from '../../components/Card'
import { Modal } from '../../components/Modal'
import { ConfirmDialog } from '../../components/ConfirmDialog'

const ROOM_STATUSES = ['ACTIVE', 'INACTIVE'] as const

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
  return (
    <div className="space-y-3">
      {formError && <p className="text-sm text-red-500">{formError}</p>}
      <div>
        <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldName')}</label>
        <input className={INPUT_CLS} value={form.roomName}
          onChange={(e) => setForm((f) => ({ ...f, roomName: e.target.value }))} />
      </div>
      <div>
        <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldType')}</label>
        <input className={INPUT_CLS} value={form.roomType}
          onChange={(e) => setForm((f) => ({ ...f, roomType: e.target.value }))} />
      </div>
      <div>
        <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldDescription')}</label>
        <textarea className={INPUT_CLS} rows={2} value={form.description ?? ''}
          onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldCapacity')}</label>
          <input type="number" min={1} className={INPUT_CLS} value={form.capacity}
            onChange={(e) => setForm((f) => ({ ...f, capacity: Number(e.target.value) }))} />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldBasePrice')}</label>
          <input type="number" min={0} step="0.01" className={INPUT_CLS} value={form.basePrice ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, basePrice: e.target.value ? Number(e.target.value) : undefined }))} />
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
      <div>
        <label className="mb-1 block text-xs font-medium text-slate-500">{t('portalPages.roomFieldStatus')}</label>
        <select className={INPUT_CLS} value={form.status ?? 'ACTIVE'}
          onChange={(e) => setForm((f) => ({ ...f, status: e.target.value as 'ACTIVE' | 'INACTIVE' }))}>
          {ROOM_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>
    </div>
  )
}

export function PortalRoomsPage() {
  const { id } = useParams<{ id: string }>()
  const { t } = useLanguage()

  const [rooms, setRooms] = useState<HotelRoomDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [addOpen, setAddOpen] = useState(false)
  const [editRoom, setEditRoom] = useState<HotelRoomDto | null>(null)
  const [deleteRoom, setDeleteRoom] = useState<HotelRoomDto | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

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
  })

  const [form, setForm] = useState<CreateHotelRoomPayload>(emptyForm())

  useEffect(() => {
    if (!id) return
    portalServicesAPI.listRooms(id)
      .then((r) => setRooms(r.data))
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [id])

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
    })
    setFormError('')
    setEditRoom(room)
  }

  async function handleAdd() {
    if (!id) return
    setSaving(true)
    setFormError('')
    try {
      const res = await portalServicesAPI.createRoom(id, form)
      setRooms((prev) => [...prev, res.data])
      setAddOpen(false)
    } catch (e) {
      setFormError(getApiErrorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  async function handleEdit() {
    if (!id || !editRoom) return
    setSaving(true)
    setFormError('')
    const patch: UpdateHotelRoomPayload = { ...form }
    try {
      const res = await portalServicesAPI.updateRoom(id, editRoom.id, patch)
      setRooms((prev) => prev.map((r) => (r.id === editRoom.id ? res.data : r)))
      setEditRoom(null)
    } catch (e) {
      setFormError(getApiErrorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete() {
    if (!id || !deleteRoom) return
    setSaving(true)
    try {
      await portalServicesAPI.deleteRoom(id, deleteRoom.id)
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
        <Link to={`/portal/listings/${id}`}
          className="text-sm text-slate-500 hover:text-slate-700 dark:hover:text-slate-300">
          ← {t('portalPages.listingEditTitle')}
        </Link>
        <button onClick={openAdd} className="dashboard-btn-primary">
          {t('portalPages.roomAdd')}
        </button>
      </div>

      <Card>
        <h1 className="mb-4 text-lg font-semibold">{t('portalPages.roomsTitle')}</h1>

        {error && <p className="text-sm text-red-500">{error}</p>}
        {loading && <p className="text-sm text-slate-400">Loading…</p>}

        {!loading && rooms.length === 0 && (
          <p className="text-sm text-slate-400">{t('portalPages.roomsEmpty')}</p>
        )}

        {rooms.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-left text-xs font-medium text-slate-400 dark:border-slate-700">
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldName')}</th>
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldType')}</th>
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldCapacity')}</th>
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldBasePrice')}</th>
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldQuantityAvailable')}/{t('portalPages.roomFieldQuantityTotal')}</th>
                  <th className="pb-2 pr-4">{t('portalPages.roomFieldStatus')}</th>
                  <th className="pb-2"></th>
                </tr>
              </thead>
              <tbody>
                {rooms.map((room) => (
                  <tr key={room.id} className="border-b border-slate-50 last:border-0 dark:border-slate-800">
                    <td className="py-2 pr-4 font-medium">{room.roomName}</td>
                    <td className="py-2 pr-4 text-slate-500">{room.roomType}</td>
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
                        <button onClick={() => openEdit(room)}
                          className="text-xs text-primary hover:underline">{t('portalPages.roomEdit')}</button>
                        <button onClick={() => setDeleteRoom(room)}
                          className="text-xs text-red-500 hover:underline">{t('portalPages.roomDelete')}</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

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

      <ConfirmDialog
        open={!!deleteRoom}
        onClose={() => setDeleteRoom(null)}
        title={t('portalPages.roomDelete')}
        description={t('portalPages.roomDeleteConfirm')}
        confirmLabel={t('portalPages.roomDelete')}
        variant="danger"
        onConfirm={handleDelete}
      />
    </>
  )
}
