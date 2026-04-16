/**
 * Logo component - uses the full logo image (suitcase icon + Ziyara wordmark).
 * Default: /logo.png. Override with logoImageSrc for custom asset.
 */

interface LogoProps {
  className?: string
  logoImageSrc?: string | null
  compact?: boolean
}

const DEFAULT_LOGO = '/logo.png'

export function Logo({ className = '', logoImageSrc = DEFAULT_LOGO, compact = false }: LogoProps) {
  return (
    <a
      href="/"
      className={`flex items-center no-underline ${className}`}
      aria-label="Ziyara home"
    >
      <img
        src={logoImageSrc ?? DEFAULT_LOGO}
        alt="Ziyara"
        className={compact ? 'h-10 w-auto' : 'h-14 w-auto'}
      />
    </a>
  )
}
