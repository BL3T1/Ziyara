/**
 * Shared date-formatting utilities.
 * All dashboard surfaces should use these helpers to ensure a consistent
 * date/time presentation across pages (previously three different inline styles
 * produced M/D/YYYY, YYYY-MM-DD, and DD Mon YYYY on different pages).
 *
 * Uses Intl.DateTimeFormat so output is automatically locale-aware
 * (en-GB for English, ar-SA for Arabic).
 */

function toLocaleTag(locale: string): string {
  return locale === 'ar' ? 'ar-SA' : 'en-GB'
}

/**
 * Format a date-only value (date string, ISO string, or Date object).
 * Output example: "23 May 2026" (en) / "٢٣ مايو ٢٠٢٦" (ar).
 */
export function formatDate(
  raw: string | Date | null | undefined,
  locale: string,
): string {
  if (!raw) return '—'
  try {
    return new Intl.DateTimeFormat(toLocaleTag(locale), {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    }).format(new Date(raw))
  } catch {
    return String(raw)
  }
}

/**
 * Format a date + time value (ISO string, Date, etc.).
 * Output example: "23 May 2026, 09:48" (en) / "٢٣ مايو ٢٠٢٦، ٠٩:٤٨" (ar).
 */
export function formatDateTime(
  raw: string | Date | null | undefined,
  locale: string,
): string {
  if (!raw) return '—'
  try {
    return new Intl.DateTimeFormat(toLocaleTag(locale), {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(raw))
  } catch {
    return String(raw)
  }
}
