import { describe, it, expect } from 'vitest'
import { safeRedirect } from './safeRedirect'

describe('safeRedirect', () => {
  const fallback = '/dashboard'

  it('allows internal paths', () => {
    expect(safeRedirect('/login', fallback)).toBe('/login')
    expect(safeRedirect('/portal', fallback)).toBe('/portal')
    expect(safeRedirect('/services/123', fallback)).toBe('/services/123')
  })

  it('rejects absolute URLs and protocol-relative URLs', () => {
    expect(safeRedirect('https://evil.com', fallback)).toBe(fallback)
    expect(safeRedirect('http://evil.com', fallback)).toBe(fallback)
    expect(safeRedirect('HTTP://evil.com', fallback)).toBe(fallback)
    expect(safeRedirect('HTTPS://evil.com', fallback)).toBe(fallback)
    expect(safeRedirect('//evil.com', fallback)).toBe(fallback)
  })

  it('trims whitespace and allows valid internal paths', () => {
    expect(safeRedirect('  /settings  ', fallback)).toBe('/settings')
  })

  it('rejects scripting schemes', () => {
    expect(safeRedirect('javascript:alert(1)', fallback)).toBe(fallback)
    expect(safeRedirect('data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==', fallback)).toBe(fallback)
    expect(safeRedirect('vbscript:msgbox(1)', fallback)).toBe(fallback)
    expect(safeRedirect('file:///etc/passwd', fallback)).toBe(fallback)
  })

  it('falls back for non-string and empty values', () => {
    expect(safeRedirect(undefined, fallback)).toBe(fallback)
    expect(safeRedirect(null, fallback)).toBe(fallback)
    expect(safeRedirect('   ', fallback)).toBe(fallback)
  })
})

