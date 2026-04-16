import { describe, expect, it } from 'vitest'
import { SIDEBAR_SECTIONS, filterSectionsByVisibleIds, getSidebarSectionsForRole } from './sidebar'

describe('filterSectionsByVisibleIds', () => {
  it('preserves global order and groups sections by first occurrence', () => {
    const out = filterSectionsByVisibleIds(SIDEBAR_SECTIONS, ['bookings', 'dashboard', 'hotels'])
    expect(out.map((s) => s.id)).toEqual(['management', 'main', 'services'])
    expect(out[0].items.map((i) => i.id)).toEqual(['bookings'])
    expect(out[1].items.map((i) => i.id)).toEqual(['dashboard'])
    expect(out[2].items.map((i) => i.id)).toEqual(['hotels'])
  })

  it('returns empty when ids list empty', () => {
    expect(filterSectionsByVisibleIds(SIDEBAR_SECTIONS, [])).toEqual([])
  })

  it('skips unknown ids', () => {
    const out = filterSectionsByVisibleIds(SIDEBAR_SECTIONS, ['dashboard', 'nonexistent'])
    expect(out.length).toBe(1)
    expect(out[0].items.map((i) => i.id)).toEqual(['dashboard'])
  })

  it('dedupes repeated ids', () => {
    const out = filterSectionsByVisibleIds(SIDEBAR_SECTIONS, ['dashboard', 'dashboard'])
    expect(out[0].items).toHaveLength(1)
  })
})

describe('getSidebarSectionsForRole', () => {
  it('finance role excludes admin-only items', () => {
    const sections = getSidebarSectionsForRole('finance')
    const ids = sections.flatMap((s) => s.items.map((i) => i.id))
    expect(ids).not.toContain('roles')
    expect(ids).toContain('bookings')
  })
})
