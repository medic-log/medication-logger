import { useEffect, useRef } from 'react'
import type { TutorialTarget } from '../components/TutorialTarget'

const SCRIM_ALPHA = 0.6
const MOVE_MILLIS = 250
const PADDING = 8
const CORNER = 16

interface Rect {
  left: number
  top: number
  width: number
  height: number
}

const ZERO: Rect = { left: 0, top: 0, width: 0, height: 0 }

/** Where an element shows on screen, clipped to what's visible, so one scrolled away counts as off screen. */
function visibleBoundsOf(element: Element): Rect | null {
  const box = element.getBoundingClientRect()
  let [left, top, right, bottom] = [box.left, box.top, box.right, box.bottom]
  for (let parent = element.parentElement; parent; parent = parent.parentElement) {
    const overflow = getComputedStyle(parent).overflowY
    if (overflow === 'auto' || overflow === 'scroll' || overflow === 'hidden') {
      const clip = parent.getBoundingClientRect()
      left = Math.max(left, clip.left)
      top = Math.max(top, clip.top)
      right = Math.min(right, clip.right)
      bottom = Math.min(bottom, clip.bottom)
    }
  }
  return right > left && bottom > top ? { left, top, width: right - left, height: bottom - top } : null
}

/** Where [target] shows on screen: around every visible element marked with it, or null when none is. */
function visibleBounds(target: TutorialTarget): Rect | null {
  const parts = [...document.querySelectorAll(`[data-tutorial-target~="${target}"]`)]
    .map(visibleBoundsOf)
    .filter((it): it is Rect => it !== null)
  if (parts.length === 0) return null
  const left = Math.min(...parts.map(it => it.left))
  const top = Math.min(...parts.map(it => it.top))
  const right = Math.max(...parts.map(it => it.left + it.width))
  const bottom = Math.max(...parts.map(it => it.top + it.height))
  return { left, top, width: right - left, height: bottom - top }
}

// Material's FastOutSlowIn, as Compose's tween() uses
function ease(t: number): number {
  if (t >= 1) return 1
  // cubic-bezier(0.4, 0, 0.2, 1), solved for x by bisection
  let [low, high] = [0, 1]
  for (let i = 0; i < 20; i++) {
    const mid = (low + high) / 2
    const x = 3 * (1 - mid) ** 2 * mid * 0.4 + 3 * (1 - mid) * mid ** 2 * 0.2 + mid ** 3
    if (x < t) low = mid
    else high = mid
  }
  const s = (low + high) / 2
  return 3 * (1 - s) * s ** 2 + s ** 3
}

const lerp = (from: Rect, to: Rect, t: number): Rect => ({
  left: from.left + (to.left - from.left) * t,
  top: from.top + (to.top - from.top) * t,
  width: from.width + (to.width - from.width) * t,
  height: from.height + (to.height - from.height) * t,
})

const isSame = (a: Rect, b: Rect) =>
  Math.abs(a.left - b.left) < 0.5 && Math.abs(a.top - b.top) < 0.5 &&
  Math.abs(a.width - b.width) < 0.5 && Math.abs(a.height - b.height) < 0.5

/**
 * Dims the screen around the tutorial: spotlights on [highlight] and on the current [tab], each with an outline,
 * and the tutorial card left clear. It only draws; the app behind is made inert while the tutorial shows.
 * A spotlight without a target closes to the middle of the card, so the next grows from there.
 */
export function TutorialOverlay({ highlight, tab }: { highlight: TutorialTarget | null; tab: TutorialTarget | null }) {
  const svg = useRef<SVGSVGElement>(null)
  const targets = useRef({ highlight, tab })
  targets.current = { highlight, tab }

  useEffect(() => {
    const spotlights = [
      { key: 'highlight' as const, from: ZERO, to: ZERO, shown: ZERO, start: 0 },
      { key: 'tab' as const, from: ZERO, to: ZERO, shown: ZERO, start: 0 },
    ]
    let frame = 0
    const draw = (time: number) => {
      const root = svg.current
      if (root) {
        const card = visibleBounds('TUTORIAL_CARD')
        const cardCenter = card
          ? { left: card.left + card.width / 2, top: card.top + card.height / 2, width: 0, height: 0 }
          : ZERO
        const cardHole = root.querySelector('.card-hole')!
        setRect(cardHole, card ?? ZERO)
        for (const spotlight of spotlights) {
          const target = targets.current[spotlight.key]
          const bounds = (target && visibleBounds(target)) ?? cardCenter
          if (!isSame(bounds, spotlight.to)) {
            spotlight.from = spotlight.shown
            spotlight.to = bounds
            spotlight.start = time
          }
          const progress = Math.min((time - spotlight.start) / MOVE_MILLIS, 1)
          spotlight.shown = lerp(spotlight.from, spotlight.to, ease(progress))
          const shown = spotlight.shown
          const hole = shown.width > 0
            ? { left: shown.left - PADDING, top: shown.top - PADDING, width: shown.width + 2 * PADDING, height: shown.height + 2 * PADDING }
            : ZERO
          root.querySelectorAll(`.${spotlight.key}`).forEach(element => setRect(element, hole))
        }
      }
      frame = requestAnimationFrame(draw)
    }
    frame = requestAnimationFrame(draw)
    return () => cancelAnimationFrame(frame)
  }, [])

  return (
    <svg ref={svg} className="tutorial-overlay" width="100%" height="100%" aria-hidden="true">
      <defs>
        <mask id="tutorial-mask">
          <rect width="100%" height="100%" fill="white" />
          <rect className="card-hole" fill="black" />
          <rect className="highlight" rx={CORNER} fill="black" />
          <rect className="tab" rx={CORNER} fill="black" />
        </mask>
      </defs>
      <rect width="100%" height="100%" fill="var(--scrim)" fillOpacity={SCRIM_ALPHA} mask="url(#tutorial-mask)" />
      <rect className="highlight" rx={CORNER} fill="none" stroke="var(--primary)" strokeWidth={2} />
      <rect className="tab" rx={CORNER} fill="none" stroke="var(--primary)" strokeWidth={2} />
    </svg>
  )
}

function setRect(element: Element, rect: Rect) {
  element.setAttribute('x', String(rect.left))
  element.setAttribute('y', String(rect.top))
  element.setAttribute('width', String(Math.max(rect.width, 0)))
  element.setAttribute('height', String(Math.max(rect.height, 0)))
  element.setAttribute('visibility', rect.width > 0 ? 'visible' : 'hidden')
}
