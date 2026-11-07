import qrcode from 'qrcode-generator'
import { useMemo } from 'react'
import { squirclePath } from './squircle'

/**
 * The blank border around the code, counted in modules. The standard asks for four; the extra
 * one only gives the code a little more room on its ground.
 */
const MARGIN_MODULES = 5
/** The side of a finder pattern — the three big marks in the corners — counted in modules. */
const FINDER_MODULES = 7
/** How much of a module the rounding takes off its corners. */
const MODULE_CORNER_FRACTION = 0.3

/**
 * A scannable QR code for [content], [size] pixels square: the Android app's QrCode, with error correction M,
 * modules snapped to whole pixels and centred, rounded marks on a squircle ground, the finders as a ring around
 * an eye. Dark on light whatever the theme, as many readers refuse a code drawn the other way round.
 */
export function QrCode({ content, size, label }: { content: string; size: number; label?: string }) {
  const matrix = useMemo(() => {
    const code = qrcode(0, 'M')
    code.addData(content)
    code.make()
    const count = code.getModuleCount()
    return { count, isDark: (row: number, column: number) => code.isDark(row, column) }
  }, [content])
  const span = matrix.count + 2 * MARGIN_MODULES
  const module = Math.floor(size / span)
  const origin = (size - module * span) / 2 + MARGIN_MODULES * module
  const finders = [
    [0, 0],
    [matrix.count - FINDER_MODULES, 0],
    [0, matrix.count - FINDER_MODULES],
  ]
  const isInFinder = (row: number, column: number) =>
    finders.some(([x, y]) => column - x >= 0 && column - x < FINDER_MODULES && row - y >= 0 && row - y < FINDER_MODULES)
  const modules: string[] = []
  for (let row = 0; row < matrix.count; row++) {
    for (let column = 0; column < matrix.count; column++) {
      if (!matrix.isDark(row, column) || isInFinder(row, column)) continue
      modules.push(`${origin + column * module},${origin + row * module}`)
    }
  }
  const corner = module * MODULE_CORNER_FRACTION
  return (
    <svg
      className="qr"
      width={size}
      height={size}
      viewBox={`0 0 ${size} ${size}`}
      role={label ? 'img' : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
    >
      <path d={squirclePath(size, size)} fill="#ffffff" />
      {modules.map(position => {
        const [x, y] = position.split(',')
        return <rect key={position} x={x} y={y} width={module} height={module} rx={corner} fill="#000000" />
      })}
      {finders.map(([column, row]) => {
        const x = origin + column * module
        const y = origin + row * module
        // A stroke straddles its line, so the ring is inset by half a module to stay inside the seven
        return (
          <g key={`${column},${row}`}>
            <rect
              x={x + module / 2}
              y={y + module / 2}
              width={(FINDER_MODULES - 1) * module}
              height={(FINDER_MODULES - 1) * module}
              rx={2 * module}
              fill="none"
              stroke="#000000"
              strokeWidth={module}
            />
            <rect x={x + 2 * module} y={y + 2 * module} width={3 * module} height={3 * module} rx={module} fill="#000000" />
          </g>
        )
      })}
    </svg>
  )
}
