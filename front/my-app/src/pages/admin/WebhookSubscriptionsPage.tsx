import { useCallback, useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { getApiErrorMessage, webhooksAPI } from '../../services/api'
import { useToast } from '../../context/ToastContext'
import { Card } from '../../components/Card'
import { Modal } from '../../components/Modal'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import type { WebhookDeliveryDto, WebhookSubscriptionDto } from '../../types/api'
import { usePermission } from '../../hooks/usePermission'

const FALLBACK_EVENTS = ['booking.created', 'booking.cancelled', 'payment.completed', 'payout.processed', 'content.approved', 'provider.approved']

function StatusBadge({ status }: { status: string }) {
  const base = 'inline-flex items-center px-2 py-0.5 rounded text-xs font-medium'
  if (status === 'DELIVERED') return <span className={`${base} bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200`}>{status}</span>
  if (status === 'FAILED') return <span className={`${base} bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200`}>{status}</span>
  return <span className={`${base} bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200`}>{status}</span>
}

export function WebhookSubscriptionsPage() {
  const { t } = useLanguage()
  useDocumentMeta({ title: `${t('title.webhooks')} — Ziyara` })
  const canWrite = usePermission('webhooks:write')
  const toast = useToast()
  const [subs, setSubs] = useState<WebhookSubscriptionDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Supported events (fetched from backend, fallback until loaded)
  const [supportedEvents, setSupportedEvents] = useState<string[]>(FALLBACK_EVENTS)
  useEffect(() => {
    webhooksAPI.listEvents()
      .then((r) => {
        if (Array.isArray(r.data) && r.data.length > 0) {
          setSupportedEvents(r.data as string[])
          setNewEvents((prev) => prev.length === 0 ? [(r.data as string[])[0]] : prev)
        }
      })
      .catch(() => {})
  }, [])

  // Create modal
  const [showCreate, setShowCreate] = useState(false)
  const [newName, setNewName] = useState('')
  const [newUrl, setNewUrl] = useState('')
  const [newEvents, setNewEvents] = useState<string[]>([])
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)
  const [createdSecret, setCreatedSecret] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  // Delete confirm
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null)

  // Ping loading state (keyed by subscription id)
  const [pingingId, setPingingId] = useState<string | null>(null)

  // Delivery log modal
  const [deliverySub, setDeliverySub] = useState<WebhookSubscriptionDto | null>(null)
  const [deliveries, setDeliveries] = useState<WebhookDeliveryDto[]>([])
  const [deliveriesLoading, setDeliveriesLoading] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    webhooksAPI
      .list()
      .then((r) => setSubs(Array.isArray(r.data) ? r.data : []))
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  function resetCreate() {
    setNewName('')
    setNewUrl('')
    setNewEvents(['booking.created'])
    setCreateError(null)
    setCreatedSecret(null)
  }

  function handleCreate() {
    if (!newName.trim() || !newUrl.trim() || newEvents.length === 0) {
      setCreateError(t('webhooksPage.createValidation'))
      return
    }
    setCreating(true)
    setCreateError(null)
    webhooksAPI
      .create({ name: newName.trim(), url: newUrl.trim(), events: newEvents })
      .then((r) => {
        setCreatedSecret(r.data.secret ?? null)
        load()
      })
      .catch((e) => setCreateError(getApiErrorMessage(e)))
      .finally(() => setCreating(false))
  }

  function toggleEvent(event: string) {
    setNewEvents((prev) =>
      prev.includes(event) ? prev.filter((e) => e !== event) : [...prev, event],
    )
  }

  function handleDelete(id: string) {
    setPendingDeleteId(id)
  }

  function executeDelete(id: string) {
    webhooksAPI
      .delete(id)
      .then(() => load())
      .catch((e) => setError(getApiErrorMessage(e)))
  }

  function handleToggleActive(id: string, current: boolean) {
    webhooksAPI
      .setActive(id, !current)
      .then(() => load())
      .catch((e) => setError(getApiErrorMessage(e)))
  }

  function handlePing(id: string) {
    setPingingId(id)
    webhooksAPI
      .ping(id)
      .then(() => toast.success(t('webhooksPage.pingDispatched')))
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setPingingId(null))
  }

  function handleCopySecret() {
    if (!createdSecret) return
    navigator.clipboard.writeText(createdSecret).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }

  function openDeliveries(sub: WebhookSubscriptionDto) {
    setDeliverySub(sub)
    setDeliveriesLoading(true)
    webhooksAPI
      .listDeliveries(sub.id)
      .then((r) => setDeliveries(Array.isArray(r.data) ? r.data : []))
      .catch(() => setDeliveries([]))
      .finally(() => setDeliveriesLoading(false))
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('webhooksPage.title')}</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">{t('webhooksPage.description')}</p>
        </div>
        {canWrite && (
          <button
            onClick={() => { resetCreate(); setShowCreate(true) }}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:opacity-90"
          >
            + {t('webhooksPage.createBtn')}
          </button>
        )}
      </div>

      <div role="status" aria-live="polite" aria-atomic="true">
        {error && (
          <div className="rounded-lg bg-red-50 p-3 text-sm text-red-700 dark:bg-red-900/30 dark:text-red-300">{error}</div>
        )}
      </div>

      {loading ? (
        <div className="py-12 text-center text-sm text-gray-500">{t('webhooksPage.loading')}</div>
      ) : subs.length === 0 ? (
        <Card>
          <div className="py-12 text-center text-sm text-gray-500 dark:text-gray-400">
            {t('webhooksPage.empty')}
          </div>
        </Card>
      ) : (
        <div className="space-y-3">
          {subs.map((sub) => (
            <Card key={sub.id}>
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-semibold text-gray-900 dark:text-white">{sub.name}</span>
                    <span className={`inline-flex items-center rounded px-2 py-0.5 text-xs font-medium ${sub.active ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200' : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'}`}>
                      {sub.active ? t('webhooksPage.active') : t('webhooksPage.inactive')}
                    </span>
                  </div>
                  <p className="mt-1 break-all text-sm text-gray-500 dark:text-gray-400">{sub.url}</p>
                  <div className="mt-2 flex flex-wrap gap-1">
                    {sub.events.map((ev) => (
                      <span key={ev} className="rounded bg-blue-50 px-2 py-0.5 text-xs text-blue-700 dark:bg-blue-900/30 dark:text-blue-300">
                        {ev}
                      </span>
                    ))}
                  </div>
                  <p className="mt-1 text-xs text-gray-400">
                    {t('webhooksPage.created')}: {new Date(sub.createdAt).toLocaleDateString()}
                  </p>
                </div>
                <div className="flex shrink-0 flex-wrap gap-2">
                  {canWrite && (
                    <button
                      onClick={() => handleToggleActive(sub.id, sub.active)}
                      className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-200 dark:hover:bg-gray-600"
                    >
                      {sub.active ? t('webhooksPage.disable') : t('webhooksPage.enable')}
                    </button>
                  )}
                  {canWrite && (
                    <button
                      onClick={() => handlePing(sub.id)}
                      disabled={pingingId === sub.id}
                      className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-200 dark:hover:bg-gray-600"
                    >
                      {pingingId === sub.id ? '…' : t('webhooksPage.ping')}
                    </button>
                  )}
                  <button
                    onClick={() => openDeliveries(sub)}
                    className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-200 dark:hover:bg-gray-600"
                  >
                    {t('webhooksPage.deliveries')}
                  </button>
                  {canWrite && (
                    <button
                      onClick={() => handleDelete(sub.id)}
                      className="rounded-md border border-red-300 bg-white px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 dark:border-red-700 dark:bg-gray-700 dark:text-red-400"
                    >
                      {t('webhooksPage.delete')}
                    </button>
                  )}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Create Modal */}
      {showCreate && (
        <Modal
          open={showCreate}
          onClose={() => { setShowCreate(false); resetCreate() }}
          title={t('webhooksPage.createTitle')}
        >
          <div className="space-y-4">
            {createdSecret ? (
              <div className="space-y-3">
                <div className="rounded-lg bg-green-50 p-3 text-sm text-green-800 dark:bg-green-900/30 dark:text-green-200">
                  {t('webhooksPage.secretNotice')}
                </div>
                <div>
                  <p className="mb-1 text-xs font-medium text-gray-600 dark:text-gray-400">{t('webhooksPage.secretLabel')}</p>
                  <code className="block break-all rounded bg-gray-100 p-3 text-xs text-gray-800 dark:bg-gray-800 dark:text-gray-200">
                    {createdSecret}
                  </code>
                  <button
                    onClick={handleCopySecret}
                    className="mt-2 w-full rounded-lg border border-gray-300 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-200 dark:hover:bg-gray-700"
                  >
                    {copied ? t('webhooksPage.copied') : t('webhooksPage.copySecret')}
                  </button>
                </div>
                <button
                  onClick={() => { setShowCreate(false); resetCreate() }}
                  className="w-full rounded-lg bg-primary py-2 text-sm font-medium text-white hover:opacity-90"
                >
                  {t('webhooksPage.done')}
                </button>
              </div>
            ) : (
              <>
                <div>
                  <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                    {t('webhooksPage.fieldName')}
                  </label>
                  <input
                    value={newName}
                    onChange={(e) => setNewName(e.target.value)}
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    placeholder={t('webhooksPage.fieldNamePlaceholder')}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                    {t('webhooksPage.fieldUrl')}
                  </label>
                  <input
                    value={newUrl}
                    onChange={(e) => setNewUrl(e.target.value)}
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    placeholder="https://your-server.com/webhooks/ziyara"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                    {t('webhooksPage.fieldEvents')}
                  </label>
                  <div className="space-y-2">
                    {supportedEvents.map((ev) => (
                      <label key={ev} className="flex cursor-pointer items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={newEvents.includes(ev)}
                          onChange={() => toggleEvent(ev)}
                          className="h-4 w-4 rounded border-gray-300 text-primary"
                        />
                        <span className="font-mono text-gray-700 dark:text-gray-300">{ev}</span>
                      </label>
                    ))}
                  </div>
                </div>
                {createError && (
                  <p className="text-sm text-red-600 dark:text-red-400">{createError}</p>
                )}
                <div className="flex gap-3">
                  <button
                    onClick={() => { setShowCreate(false); resetCreate() }}
                    className="flex-1 rounded-lg border border-gray-300 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-200 dark:hover:bg-gray-700"
                  >
                    {t('webhooksPage.cancel')}
                  </button>
                  <button
                    onClick={handleCreate}
                    disabled={creating}
                    className="flex-1 rounded-lg bg-primary py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50"
                  >
                    {creating ? t('webhooksPage.creating') : t('webhooksPage.createBtn')}
                  </button>
                </div>
              </>
            )}
          </div>
        </Modal>
      )}

      <ConfirmDialog
        open={!!pendingDeleteId}
        onClose={() => setPendingDeleteId(null)}
        title={t('webhooksPage.delete')}
        description={t('webhooksPage.deleteConfirm')}
        confirmLabel={t('webhooksPage.delete')}
        variant="danger"
        onConfirm={() => executeDelete(pendingDeleteId!)}
      />

      {/* Delivery Log Modal */}
      {deliverySub && (
        <Modal
          open={!!deliverySub}
          onClose={() => { setDeliverySub(null); setDeliveries([]) }}
          title={`${t('webhooksPage.deliveriesTitle')}: ${deliverySub.name}`}
        >
          {deliveriesLoading ? (
            <div className="py-8 text-center text-sm text-gray-500">{t('webhooksPage.loading')}</div>
          ) : deliveries.length === 0 ? (
            <div className="py-8 text-center text-sm text-gray-500">{t('webhooksPage.noDeliveries')}</div>
          ) : (
            <div className="space-y-2 max-h-96 overflow-y-auto">
              {deliveries.map((d) => (
                <div key={d.id} className="rounded-lg border border-gray-200 p-3 dark:border-gray-700">
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-mono text-xs text-gray-600 dark:text-gray-400">{d.event}</span>
                    <StatusBadge status={d.status} />
                  </div>
                  <div className="mt-1 flex items-center gap-3 text-xs text-gray-500">
                    {d.httpStatus && <span>HTTP {d.httpStatus}</span>}
                    <span>{t('webhooksPage.attempts')}: {d.attemptCount}</span>
                    <span>{new Date(d.createdAt).toLocaleString()}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Modal>
      )}
    </div>
  )
}
