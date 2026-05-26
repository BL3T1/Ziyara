/**
 * Create service provider (partner) — modal shell; Super Admin path activates immediately; Sales path stays pending.
 */

import { CreateProviderForm, type CreateProviderVariant } from './CreateProviderForm'
import { Modal } from './Modal'
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

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={t('createProviderModal.title')}
      description={
        variant === 'sales'
          ? t('createProviderModal.introSales')
          : t('createProviderModal.introSuper')
      }
    >
      <CreateProviderForm
        variant={variant}
        presetServiceType={presetServiceType}
        onCancel={onClose}
        onCreated={onCreated}
        successCloseMs={1200}
      />
    </Modal>
  )
}
