/**
 * Create service provider (partner) — modal shell; Super Admin path activates immediately; Sales path stays pending.
 */

import { CreateProviderForm, type CreateProviderVariant } from './CreateProviderForm'
import { useLanguage } from '../context/LanguageContext'
import type { ServiceTypeDto } from '../types/api'

export type { CreateProviderVariant }

export function CreateProviderModal({
  open,
  onClose,
  variant,
  onCreated,
  presetServiceType,
}: {
  open: boolean
  onClose: () => void
  variant: CreateProviderVariant
  onCreated?: () => void
  presetServiceType?: ServiceTypeDto
}) {
  const { t } = useLanguage()

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      onClick={onClose}
    >
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-700 dark:bg-slate-800"
        onClick={(ev) => ev.stopPropagation()}
      >
        <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">
          {t('createProviderModal.title')}
        </h3>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          {variant === 'sales' ? t('createProviderModal.introSales') : t('createProviderModal.introSuper')}
        </p>

        <CreateProviderForm
          variant={variant}
          presetServiceType={presetServiceType}
          onCancel={onClose}
          onCreated={onCreated}
          successCloseMs={1200}
        />
      </div>
    </div>
  )
}
