/** Maps API service type to legacy `/{segment}/:id` path segment (redirects to `/services/{segment}` in company app). */
export function serviceTypeToListingsPath(type: string | undefined): string {
  const u = (type ?? '').toUpperCase()
  if (u === 'HOTEL') return 'hotels'
  if (u === 'RESORT') return 'resorts'
  if (u === 'RESTAURANT') return 'restaurants'
  if (u === 'TAXI') return 'taxis'
  return 'trips'
}
