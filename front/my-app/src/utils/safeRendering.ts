/**
 * Lightweight client-side hardening for untrusted data coming from the API.
 *
 * Note: React already escapes text nodes, but we additionally:
 * - sanitize any potentially HTML-like strings back to plain text
 * - validate URLs before assigning them to sensitive attributes like `img src`
 *
 * `safeImageUrl` allows `https://` / `http://` so CDN or external image hosts keep working;
 * dangerous schemes (`javascript:`, `data:`, etc.) are rejected.
 */

export function sanitizeText(input: unknown): string {
  if (typeof input !== 'string') return ''
  // Remove common active content first, then strip all remaining tags.
  // This is best-effort protection for DOM sinks that might be flagged by Snyk.
  const noActiveContent = input.replace(/<(script|style)\b[^>]*>[\s\S]*?<\/\1>/gi, '')
  // Strip HTML tags. If the backend legitimately stores "<" characters, they will be removed.
  return noActiveContent.replace(/<[^>]*>/g, '')
}

export function safeImageUrl(input: unknown): string | null {
  if (typeof input !== 'string') return null
  const url = input.trim()
  if (!url) return null

  // Reject protocol-relative URLs and any non-http(s) schemes.
  if (url.startsWith('//')) return null
  if (url.startsWith('javascript:') || url.startsWith('data:') || url.startsWith('file:') || url.startsWith('vbscript:')) {
    return null
  }

  // Allow relative app media URLs and absolute HTTP(S) URLs.
  if (url.startsWith('/')) return url
  if (url.startsWith('http://') || url.startsWith('https://')) return url

  return null
}

