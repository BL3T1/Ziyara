import { useState } from 'react'
import { Modal } from './Modal'
import { useLanguage } from '../context/LanguageContext'

interface ConfirmDialogProps {
  open: boolean
  onClose: () => void
  title: string
  description?: string
  confirmLabel?: string
  variant?: 'danger' | 'default'
  onConfirm: () => Promise<void> | void
  reason?: {
    label: string
    placeholder?: string
    required?: boolean
    value: string
    onChange: (v: string) => void
    minLength?: number
  }
}

export function ConfirmDialog({
  open,
  onClose,
  title,
  description,
  confirmLabel,
  variant = 'default',
  onConfirm,
  reason,
}: ConfirmDialogProps) {
  const { t } = useLanguage()
  const [loading, setLoading] = useState(false)

  const handleConfirm = async () => {
    setLoading(true)
    try {
      await onConfirm()
      onClose()
    } finally {
      setLoading(false)
    }
  }

  const reasonInvalid =
    reason?.required &&
    (reason.value.trim().length === 0 ||
      (reason.minLength != null && reason.value.trim().length < reason.minLength))

  const confirmBtnClass =
    variant === 'danger'
      ? 'inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold text-white transition-all duration-150 bg-red-600 hover:bg-red-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500/50 active:scale-[0.97] disabled:opacity-50'
      : 'dashboard-btn-primary disabled:opacity-50'

  return (
    <Modal
      open={open}
      onClose={loading ? () => {} : onClose}
      title={title}
      description={description}
      size="sm"
      footer={
        <>
          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="dashboard-btn-secondary"
          >
            {t('confirm.cancelBtn')}
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={loading || !!reasonInvalid}
            className={confirmBtnClass}
          >
            {loading ? t('ui.loading') : (confirmLabel ?? t('confirm.confirmBtn'))}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        {!description && (
          <p className="text-sm text-slate-600 dark:text-slate-300">{t('confirm.cannotUndo')}</p>
        )}
        {reason && (
          <div className="space-y-1.5">
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
              {reason.label}
              {reason.required && (
                <span className="ml-0.5 text-red-500" aria-hidden="true">*</span>
              )}
            </label>
            <textarea
              value={reason.value}
              onChange={(e) => reason.onChange(e.target.value)}
              placeholder={reason.placeholder ?? t('confirm.reasonPlaceholder')}
              rows={3}
              className="modal-textarea"
            />
          </div>
        )}
      </div>
    </Modal>
  )
}
