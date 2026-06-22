import { useEffect } from 'react'

interface LightboxImage { src: string; alt: string }

interface Props {
  images: LightboxImage[]
  activeIndex: number
  onClose: () => void
  onNext: () => void
  onPrev: () => void
}

export function LandingImageLightbox({ images, activeIndex, onClose, onNext, onPrev }: Props) {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
      if (e.key === 'ArrowRight') onNext()
      if (e.key === 'ArrowLeft') onPrev()
    }
    document.addEventListener('keydown', handler)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', handler)
      document.body.style.overflow = ''
    }
  }, [onClose, onNext, onPrev])

  const current = images[activeIndex]
  if (!current) return null

  return (
    <div
      className="lp-lightbox"
      role="dialog"
      aria-modal
      aria-label="Image viewer"
      onClick={onClose}
    >
      <button
        type="button"
        className="lp-lightbox__close"
        onClick={onClose}
        aria-label="Close"
      >
        ✕
      </button>

      {images.length > 1 && (
        <button
          type="button"
          className="lp-lightbox__prev"
          onClick={(e) => { e.stopPropagation(); onPrev() }}
          aria-label="Previous image"
        >
          ‹
        </button>
      )}

      <img
        src={current.src}
        alt={current.alt}
        className="lp-lightbox__img"
        onClick={(e) => e.stopPropagation()}
        draggable={false}
      />

      {images.length > 1 && (
        <button
          type="button"
          className="lp-lightbox__next"
          onClick={(e) => { e.stopPropagation(); onNext() }}
          aria-label="Next image"
        >
          ›
        </button>
      )}

      {images.length > 1 && (
        <p className="lp-lightbox__counter" aria-live="polite">
          {activeIndex + 1} / {images.length}
        </p>
      )}
    </div>
  )
}
