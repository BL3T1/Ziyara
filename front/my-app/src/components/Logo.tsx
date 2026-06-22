/**
 * Logo component - uses the full logo image (suitcase icon + Ziyara wordmark).
 * Default: /logo.png. Override with logoImageSrc for custom asset.
 */

interface LogoProps {
  className?: string
  logoImageSrc?: string | null
  compact?: boolean
  /** When set, logo is a control (e.g. expand collapsed sidebar) instead of the home link. */
  expandAction?: { onClick: () => void; ariaLabel: string }
}

const DEFAULT_LOGO = '/logo.png'

const logoImgClass = (compact: boolean) => (compact ? 'h-16 w-auto' : 'h-20 w-auto')

export function Logo({ className = '', logoImageSrc = DEFAULT_LOGO, compact = false, expandAction }: LogoProps) {
  if (expandAction) {
    return (
      <button
        type="button"
        onClick={expandAction.onClick}
        className={`flex cursor-pointer items-center border-0 bg-transparent p-0 no-underline outline-none transition-opacity hover:opacity-90 focus-visible:ring-2 focus-visible:ring-[rgb(172_158_120/0.45)] focus-visible:ring-offset-2 focus-visible:ring-offset-slate-900 ${className}`}
        aria-label={expandAction.ariaLabel}
      >
        <img src={logoImageSrc ?? DEFAULT_LOGO} alt="" className={logoImgClass(compact)} />
      </button>
    )
  }

  const img = (
    <img
      src={logoImageSrc ?? DEFAULT_LOGO}
      alt="Ziyara"
      className={logoImgClass(compact)}
    />
  )

  return (
    <a href="/" className={`flex items-center no-underline ${className}`} aria-label="Ziyara home">
      {img}
    </a>
  )
}
