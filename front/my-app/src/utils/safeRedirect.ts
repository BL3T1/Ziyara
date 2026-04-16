/**
 * Open-redirect hardening for client-side navigation.
 *
 * React Router supports navigating to arbitrary strings; attackers may try to smuggle in
 * external URLs or dangerous schemes. We therefore allow only internal paths that start
 * with `/` and explicitly reject common external/scripting schemes.
 */
export function safeRedirect(target: unknown, fallback: string): string {
  if (typeof target !== 'string') return fallback
  const t = target.trim()
  if (!t) return fallback

  const lower = t.toLowerCase()

  // Reject protocol-relative URLs.
  if (t.startsWith('//')) return fallback

  // Reject any absolute URLs (external origins).
  if (lower.startsWith('http://') || lower.startsWith('https://')) return fallback

  // Reject scripting/data/file schemes.
  if (lower.startsWith('javascript:') || lower.startsWith('data:') || lower.startsWith('vbscript:') || lower.startsWith('file:')) {
    return fallback
  }

  // Allow only internal paths.
  return t.startsWith('/') ? t : fallback
}

