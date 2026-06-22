import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { getApiErrorMessage, portalServicesAPI } from '../../services/api'
import type {
  HotelRoomDto,
  HotelRoomStatusDto,
  CreateHotelRoomPayload,
  UpdateHotelRoomPayload,
} from '../../types/api'
import { Card } from '../../components/Card'
import { Modal } from '../../components/Modal'
import { ConfirmDialog } from '../../components/ConfirmDialog'

type RoomFormState = {
  roomType: string
  roomName: string
  description: string
  capacity: string
  basePrice: string
  currency: string
  quantityTotal: string
  quantityAvailable: string
  status: HotelRoomStatusDto
}

const EMPTY_FORM: RoomFormState = {
  roomType: '',
  roomName: '',
  description: '',
  capacity: '1',
  basePrice: '',
  currency: 'USD',
  quantityTotal: '1',
  quantityAvailable: '1',
  status: 'ACTIVE',
}

function roomToForm(r: HotelRoomDto): RoomFormState {
  return {
    roomType: r.roomType,
    roomName: r.roomName,
    description: r.description ?? '',
    capacity: String(r.capacity),
    basePrice: r.basePrice != null ? String(r.basePrice) : '',
    currency: r.currency ?? 'USD',
    quantityTotal: String(r.quantityTotal),
    quantityAvailable: String(r.quantityAvailable),
    status: r.status,
  }
}

function RoomFormFields({
  form,
  onChange,
  inputCls,
}: {
  form: RoomFormState
  onChange: (patch: Partial<RoomFormState>) => void
  inputCls: string
}) {
  const { t } = useLanguage()
  return (
    <div className="space-y-3">
      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('portalPages.roomsFieldRoomType')} *
          </label>
          <input
            required
            value={form.roomType}
            onChange={(e) => onChange({ roomType: e.target.value })}
            placeholder="e.g. Standard, Deluxe, Suite"
            className={inputCls}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('portalPages.roomsFieldRoomName')} *
          </label>
          <input
            required
            value={form.roomName}
            onChange={(e) => onChange({ roomName: e.target.value })}
            placeholder="e.g. Deluxe King Room"
            className={inputCls}
          />
        </div>
      </div>
      <div>
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
          {t('portalPages.roomsFieldDescription')}
        </label>
        <textarea
          value={form.description}
          onChange={(e) => onChange({ description: e.target.value })}
          rows={2}
          className={inputCls}
        />
      </div>
      <div className="grid gap-3 sm:grid-cols-3">
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('portalPages.roomsFieldCapacity')} *
          </label>
          <input
            required
            type="number"
            min={1}
            value={form.capacity}
            onChange={(e) => onChange({ capacity: e.target.value })}
            className={inputCls}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('portalPages.roomsFieldQtyTotal')} *
          </label>
          <input
            required
            type="number"
            min={0}
            value={form.quantityTotal}
            onChange={(e) => onChange({ quantityTotal: e.target.value })}
            className={inputCls}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('portalPages.roomsFieldQtyAvailable')} *
          </label>
          <input
            required
            type="number"
            min={0}
            value={form.quantityAvailable}
            onChange={(e) => onChange({ quantityAvailable: e.target.value })}
            className={inputCls}
          />
        </div>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('portalPages.roomsFieldBasePrice')}
          </label>
          <input
            type="number"
            min={0}
            step="0.01"
            value={form.basePrice}
            onChange={(e) => onChange({ basePrice: e.target.value })}
            className={inputCls}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('portalPages.roomsFieldCurrency')}
          </label>
          <input
            value={form.currency}
            maxLength={3}
            onChange={(e) => onChange({ currency: e.target.value.toUpperCase() })}
            className={inputCls}
          />
        </div>
      </div>
      <div>
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
          {t('portalPages.roomsFieldStatus')}
        </label>
        <select
          value={form.status}
          onChange={(e) => onChange({ status: e.target.value as HotelRoomStatusDto })}
          className={inputCls}
        >
          <option value="ACTIVE">ACTIVE</option>
          <option value="INACTIVE">INACTIVE</option>
        </select>
      </div>
    </div>
  )
}

function formToCreatePayload(f: RoomFormState): CreateHotelRoomPayload {
  return {
    roomType: f.roomType.trim(),
    roomName: f.roomName.trim(),
    description: f.description.trim() || undefined,
    capacity: parseInt(f.capacity, 10) || 1,
    basePrice: f.basePrice ? parseFloat(f.basePrice) : undefined,
    currency: f.currency.trim() || 'USD',
    quantityTotal: parseInt(f.quantityTotal, 10) || 0,
    quantityAvailable: parseInt(f.quantityAvailable, 10) || 0,
    status: f.status,
  }
}

function formToUpdatePayload(f: RoomFormState): UpdateHotelRoomPayload {
  return formToCreatePayload(f)
}

export function PortalRoomsPage() {
  const { id: serviceId } = useParams<{ id: string }>()
  const { t } = useLanguage()
  useDocumentMeta({ title: t('portalPages.roomsPageTitle') })

  const [rooms, setRooms] = useState<HotelRoomDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [addOpen, setAddOpen] = useState(false)
  const [addForm, setAddForm] = useState<RoomFormState>(EMPTY_FORM)
  const [addError, setAddError] = useState<string | null>(null)
  const [addSubmitting, setAddSubmitting] = useState(false)

  const [editRoom, setEditRoom] = useState<HotelRoomDto | null>(null)
  const [editForm, setEditForm] = useState<RoomFormState>(EMPTY_FORM)
  const [editError, setEditError] = useState<string | null>(null)
  const [editSubmitting, setEditSubmitting] = useState(false)

  const [deleteRoom, setDeleteRoom] = useState<HotelRoomDto | null>(null)

  const inputCls =
    'mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100'

  function load() {
    if (!serviceId) return
    setLoading(true)
    portalServicesAPI
      .listRooms(serviceId)
      .then((res) => setRooms(res.data as HotelRoomDto[]))
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }

  useEffect(load, [serviceId])

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault()
    if (!serviceId) return
    setAddError(null)
    setAddSubmitting(true)
    try {
      const created = (await portalServicesAPI.createRoom(serviceId, formToCreatePayload(addForm))).data as HotelRoomDto
      setRooms((prev) => [...prev, created])
      setAddOpen(false)
      setAddForm(EMPTY_FORM)
    } catch (err) {
      setAddError(getApiErrorMessage(err))
    } finally {
      setAddSubmitting(false)
    }
  }

  async function handleEdit(e: React.FormEvent) {
    e.preventDefault()
    if (!serviceId || !editRoom) return
    setEditError(null)
    setEditSubmitting(true)
    try {
      const updated = (
        await portalServicesAPI.updateRoom(serviceId, editRoom.id, formToUpdatePayload(editForm))
      ).data as HotelRoomDto
      setRooms((prev) => prev.map((r) => (r.id === updated.id ? updated : r)))
      setEditRoom(null)
    } catch (err) {
      setEditError(getApiErrorMessage(err))
    } finally {
      setEditSubmitting(false)
    }
  }

  async function handleDelete() {
    if (!serviceId || !deleteRoom) return
    try {
      await portalServicesAPI.deleteRoom(serviceId, deleteRoom.id)
      setRooms((prev) => prev.filter((r) => r.id !== deleteRoom.id))
      setDeleteRoom(null)
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
            {t('portalPages.roomsBackToListing')}
          </Link>
          <h1 className="mt-3 app-page-title">{t('portalPages.roomsPageTitle')}</h1>
        </div>
        <button
          type="button"
          onClick={() => { setAddForm(EMPTY_FORM); setAddError(null); setAddOpen(true) }}
          className="dashboard-btn-primary"
        >
          {t('portalPages.roomsAddRoom')}
        </button>
      </div>

      {error && (
        <p className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
          {error}
        </p>
      )}

      {loading ? (
        <p className="text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
      ) : rooms.length === 0 ? (
        <Card className="p-6 text-center text-sm text-slate-500 dark:text-slate-400">
          {t('portalPages.roomsNoRooms')}
        </Card>
      ) : (
        <Card className="overflow-hidden p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 dark:border-slate-700 dark:bg-slate-800/60">
                <tr>
                  {[
                    'roomsColName',
                    'roomsColType',
                    'roomsColCapacity',
                    'roomsColPrice',
                    'roomsColAvailability',
                    'roomsColStatus',
                  ].map((k) => (
                    <th
                      key={k}
                      className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400"
                    >
                      {t(`portalPages.${k}`)}
                    </th>
                  ))}
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-700/60">
                {rooms.map((room) => (
                  <tr key={room.id} className="hover:bg-slate-50 dark:hover:bg-white/[0.02]">
                    <td className="px-4 py-3 font-medium text-slate-900 dark:text-slate-100">
                      {room.roomName}
                    </td>
                    <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{room.roomType}</td>
                    <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{room.capacity}</td>
                    <td className="px-4 py-3 text-slate-600 dark:text-slate-300">
                      {room.basePrice != null
                        ? `${room.basePrice} ${room.currency ?? ''}`
                        : '—'}
                    </td>
                    <td className="px-4 py-3 text-slate-600 dark:text-slate-300">
                      {room.quantityAvailable} / {room.quantityTotal}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex rounded-full px-2 py-0.5 text-xs font-semibold ${
                          room.status === 'ACTIVE'
                            ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'
                            : 'bg-slate-100 text-slate-500 dark:bg-slate-700 dark:text-slate-400'
                        }`}
                      >
                        {room.status}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-2 justify-end">
                        <button
                          type="button"
                          onClick={() => { setEditRoom(room); setEditForm(roomToForm(room)); setEditError(null) }}
                          className="text-xs font-medium text-primary hover:underline"
                        >
                          {t('portalPages.roomsEditRoom')}
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleteRoom(room)}
                          className="text-xs font-medium text-red-600 hover:underline dark:text-red-400"
                        >
                          {t('portalPages.roomsDeleteRoom')}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Add Room Modal */}
      <Modal
        open={addOpen}
        onClose={() => setAddOpen(false)}
        title={t('portalPages.roomsAddRoom')}
        size="lg"
        footer={
          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={() => setAddOpen(false)}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="add-room-form"
              disabled={addSubmitting}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {addSubmitting ? t('ui.loading') : t('ui.save')}
            </button>
          </div>
        }
      >
        <form id="add-room-form" onSubmit={handleAdd}>
          {addError && (
            <p className="mb-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
              {addError}
            </p>
          )}
          <RoomFormFields form={addForm} onChange={(p) => setAddForm((f) => ({ ...f, ...p }))} inputCls={inputCls} />
        </form>
      </Modal>

      {/* Edit Room Modal */}
      <Modal
        open={!!editRoom}
        onClose={() => setEditRoom(null)}
        title={t('portalPages.roomsEditRoom')}
        size="lg"
        footer={
          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={() => setEditRoom(null)}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="edit-room-form"
              disabled={editSubmitting}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {editSubmitting ? t('ui.loading') : t('ui.save')}
            </button>
          </div>
        }
      >
        <form id="edit-room-form" onSubmit={handleEdit}>
          {editError && (
            <p className="mb-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
              {editError}
            </p>
          )}
          <RoomFormFields form={editForm} onChange={(p) => setEditForm((f) => ({ ...f, ...p }))} inputCls={inputCls} />
        </form>
      </Modal>

      {/* Delete Confirm */}
      <ConfirmDialog
        open={!!deleteRoom}
        onClose={() => setDeleteRoom(null)}
        title={t('portalPages.roomsDeleteRoom')}
        description={t('portalPages.roomsConfirmDelete')}
        confirmLabel={t('portalPages.roomsDeleteRoom')}
        variant="danger"
        onConfirm={handleDelete}
      />
    </>
  )
}
