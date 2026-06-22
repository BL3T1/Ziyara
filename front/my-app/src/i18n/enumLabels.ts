function humanize(value: string): string {
  return value.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())
}

function lookup(t: (key: string) => string, key: string): string | null {
  const result = t(key)
  return result === key ? null : result
}

export function statusLabel(t: (key: string) => string, value: string | null | undefined): string {
  if (!value) return '—'
  return lookup(t, `enums.status.${value}`) ?? humanize(value)
}

export function priorityLabel(t: (key: string) => string, value: string | null | undefined): string {
  if (!value) return '—'
  return lookup(t, `enums.priority.${value}`) ?? humanize(value)
}

export function paymentMethodLabel(t: (key: string) => string, value: string | null | undefined): string {
  if (!value) return '—'
  return lookup(t, `enums.paymentMethod.${value}`) ?? humanize(value)
}

/**
 * Convert a raw audit action code (e.g. PROVIDER_PASSWORD_RESET) to a
 * human-readable label. Falls back to humanize() so future/unknown codes
 * always produce something readable.
 */
export function actionLabel(t: (key: string) => string, value: string | null | undefined): string {
  if (!value) return '—'
  return lookup(t, `enums.auditAction.${value}`) ?? humanize(value)
}
