import { type ReactNode, useEffect, useRef } from 'react'

/**
 * Material's AlertDialog, as a modal <dialog>: Escape calls [onDismiss] when it's dismissible, and so does a tap
 * outside, but only when there are no [buttons] to close it with.
 * [title] can be text or a whole row. A dialog that [isForm] scrolls as a whole, title and buttons included, like the
 * Android app's FormDialog, so a tall form stays reachable while typing; others scroll only their body.
 */
export function Dialog({
  title,
  children,
  buttons,
  onDismiss,
  isDismissible = true,
  isScrolling = true,
  isForm = false,
  className,
  label,
}: {
  title?: ReactNode
  children?: ReactNode
  buttons?: ReactNode
  onDismiss: () => void
  isDismissible?: boolean
  isScrolling?: boolean
  isForm?: boolean
  className?: string
  label?: string
}) {
  const ref = useRef<HTMLDialogElement>(null)
  const dismiss = useRef(onDismiss)
  dismiss.current = onDismiss
  useEffect(() => {
    const dialog = ref.current
    if (dialog && !dialog.open) dialog.showModal()
    return () => dialog?.close()
  }, [])
  return (
    <dialog
      ref={ref}
      className={`dialog ${isForm ? 'form' : ''} ${className ?? ''}`}
      aria-label={label}
      // The dialog itself takes focus, so none of its buttons looks chosen before the user acts
      autoFocus
      tabIndex={-1}
      onCancel={event => {
        event.preventDefault()
        if (isDismissible) dismiss.current()
      }}
      onMouseDown={event => {
        // A press on the backdrop lands on the dialog element itself, outside its content box
        if (event.target !== ref.current || !isDismissible || buttons) return
        const box = ref.current.getBoundingClientRect()
        const isOutside =
          event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom
        if (isOutside) dismiss.current()
      }}
    >
      {typeof title === 'string' ? <h2 className="dialog-title">{title}</h2> : title}
      {children !== undefined && <div className={`dialog-body ${isScrolling ? '' : 'no-scroll'}`}>{children}</div>}
      {buttons && <div className="dialog-buttons">{buttons}</div>}
    </dialog>
  )
}
