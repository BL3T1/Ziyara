import { useRef, useState } from 'react'
import type { MouseEvent } from 'react'
import { Link } from 'react-router-dom'
import { LandingHeroArt } from './LandingHeroArt'

/** Primary hero art (served from `public/ziyara-hero-reference.png`). */
const HERO_SRC = '/ziyara-hero-reference.png'

export function ZiyaraHeroComposition() {
  const wrapRef = useRef<HTMLDivElement>(null)
  const [tilt, setTilt] = useState({ x: 0, y: 0 })
  const [useRaster, setUseRaster] = useState(true)

  const onMove = (e: MouseEvent<HTMLDivElement>) => {
    const el = wrapRef.current
    if (!el) return
    const r = el.getBoundingClientRect()
    const nx = (e.clientX - r.left) / r.width - 0.5
    const ny = (e.clientY - r.top) / r.height - 0.5
    setTilt({ x: ny * -1.8, y: nx * 2.6 })
  }

  const onLeave = () => setTilt({ x: 0, y: 0 })

  return (
    <div className="lp-ziyara-hero__stage">
      <div
        ref={wrapRef}
        className="lp-ziyara-hero__parallax lp-ziyara-hero__art-root"
        onMouseMove={onMove}
        onMouseLeave={onLeave}
        style={{
          transform: `perspective(820px) rotateX(${tilt.x}deg) rotateY(${tilt.y}deg)`,
        }}
      >
        <div className="lp-ziyara-hero__art-frame">
          {useRaster ? (
            <img
              className="lp-ziyara-hero__art"
              src={HERO_SRC}
              width={1024}
              height={910}
              decoding="async"
              fetchPriority="high"
              loading="eager"
              sizes="(max-width: 900px) 92vw, min(520px, 44vw)"
              alt="Ziyara: global booking platform — stays, dining, transport, and experiences."
              onError={() => setUseRaster(false)}
            />
          ) : (
            <div className="lp-ziyara-hero-fallback">
              <LandingHeroArt variant="inline" />
            </div>
          )}
          <div className="lp-ziyara-hero__hotspots" role="group" aria-label="Service shortcuts">
            <Link to="/hotels" className="lp-ziyara-hero__hotspot lp-ziyara-hero__hotspot--tl" aria-label="Hotels" />
            <Link to="/restaurants" className="lp-ziyara-hero__hotspot lp-ziyara-hero__hotspot--tr" aria-label="Dining" />
            <Link to="/taxis" className="lp-ziyara-hero__hotspot lp-ziyara-hero__hotspot--bl" aria-label="Transport" />
            <Link to="/trips" className="lp-ziyara-hero__hotspot lp-ziyara-hero__hotspot--br" aria-label="Tours & experiences" />
          </div>
        </div>
      </div>
    </div>
  )
}
