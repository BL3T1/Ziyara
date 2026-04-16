import { describe, it, expect } from 'vitest'
import { sanitizeText, safeImageUrl } from './safeRendering'

describe('sanitizeText', () => {
  it('strips HTML tags from untrusted strings', () => {
    expect(sanitizeText('<img src=x onerror=alert(1)>hello')).toBe('hello')
    expect(sanitizeText('<script>alert(1)</script>')).toBe('')
    expect(sanitizeText('hello <b>world</b>')).toBe('hello world')
  })

  it('returns empty string for non-string input', () => {
    expect(sanitizeText(undefined)).toBe('')
    expect(sanitizeText(null)).toBe('')
  })
})

describe('safeImageUrl', () => {
  it('allows relative paths and http(s) URLs', () => {
    expect(safeImageUrl('/media/services/123/x.png')).toBe('/media/services/123/x.png')
    expect(safeImageUrl('https://example.com/x.png')).toBe('https://example.com/x.png')
    expect(safeImageUrl('http://example.com/x.png')).toBe('http://example.com/x.png')
  })

  it('rejects dangerous schemes and protocol-relative URLs', () => {
    expect(safeImageUrl('javascript:alert(1)')).toBeNull()
    expect(safeImageUrl('data:image/svg+xml,<svg onload=alert(1)>')).toBeNull()
    expect(safeImageUrl('//evil.com/x.png')).toBeNull()
    expect(safeImageUrl('file:///etc/passwd')).toBeNull()
    expect(safeImageUrl('vbscript:alert(1)')).toBeNull()
  })
})

