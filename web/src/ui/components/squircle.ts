// A superellipse of this exponent is the usual squircle: flatter sides than a circle, softer corners than a square
const SQUIRCLE_EXPONENT = 4
const SQUIRCLE_POINTS = 64

const superellipse = (value: number) => Math.sign(value) * Math.abs(value) ** (2 / SQUIRCLE_EXPONENT)

/** The outline of a squircle filling a [width] × [height] box, as SVG path data. */
export function squirclePath(width: number, height: number): string {
  const points = Array.from({ length: SQUIRCLE_POINTS }, (_, index) => {
    const angle = (2 * Math.PI * index) / SQUIRCLE_POINTS
    const x = width / 2 + (width / 2) * superellipse(Math.cos(angle))
    const y = height / 2 + (height / 2) * superellipse(Math.sin(angle))
    return `${x.toFixed(2)},${y.toFixed(2)}`
  })
  return `M${points.join('L')}Z`
}

/** A CSS clip-path for a squircle filling a [size] square. */
export const squircleClip = (size: number) => `path('${squirclePath(size, size)}')`
