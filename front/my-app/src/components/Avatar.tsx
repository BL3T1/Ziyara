/**
 * Reusable avatar that shows user photo or default placeholder.
 * Uses /default-avatar.svg when no image URL is provided or when load fails.
 */

export const DEFAULT_AVATAR = '/default-avatar.svg'

interface AvatarProps {
  src?: string | null
  alt?: string
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

const sizeClasses = {
  sm: 'h-8 w-8',
  md: 'h-9 w-9',
  lg: 'h-12 w-12',
}

export function Avatar({ src, alt = '', size = 'md', className = '' }: AvatarProps) {
  const sizeClass = sizeClasses[size]
  const imgSrc = src || DEFAULT_AVATAR

  return (
    <div className={`shrink-0 overflow-hidden rounded-full ${sizeClass} ${className}`}>
      <img
        src={imgSrc}
        alt={alt}
        className="h-full w-full object-cover"
        onError={(e) => {
          e.currentTarget.src = DEFAULT_AVATAR
          e.currentTarget.onerror = null
        }}
      />
    </div>
  )
}
