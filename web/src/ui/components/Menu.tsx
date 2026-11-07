import { type ReactNode, useLayoutEffect, useRef } from 'react'

/**
 * A dropdown menu under [anchor], in the top layer so nothing covers it; a tap outside or Escape closes it.
 */
export function Menu({ anchor, onDismiss, children }: { anchor: HTMLElement; onDismiss: () => void; children: ReactNode }) {
  const ref = useRef<HTMLDivElement>(null)
  useLayoutEffect(() => {
    const menu = ref.current
    if (!menu) return
    const box = anchor.getBoundingClientRect()
    menu.showPopover?.()
    const height = menu.offsetHeight
    const below = box.bottom + height <= window.innerHeight
    menu.style.left = `${Math.min(box.left, window.innerWidth - menu.offsetWidth - 8)}px`
    menu.style.top = `${below ? box.bottom : Math.max(8, box.top - height)}px`
    menu.querySelector<HTMLElement>('[role=menuitem]')?.focus()
    const onToggle = (event: Event) => {
      if ((event as ToggleEvent).newState === 'closed') onDismiss()
    }
    menu.addEventListener('toggle', onToggle)
    return () => menu.removeEventListener('toggle', onToggle)
  }, [anchor, onDismiss])
  return (
    <div ref={ref} className="menu" popover="auto" role="menu">
      {children}
    </div>
  )
}

export const MenuItem = ({ onClick, children }: { onClick: () => void; children: ReactNode }) => (
  <button type="button" role="menuitem" className="menu-item" onClick={onClick}>
    {children}
  </button>
)
