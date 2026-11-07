import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'

const SHORT_MILLIS = 2000
const LONG_MILLIS = 3500

type ShowToast = (message: string, isLong?: boolean) => void

const ToastContext = createContext<ShowToast>(() => {})

/** Shows a short message, like Android's Toast. */
export const useToast = () => useContext(ToastContext)

/** Holds the toast on screen, in the top layer so it shows over dialogs too. */
export function ToastHost({ children }: { children: React.ReactNode }) {
  const [toast, setToast] = useState<{ message: string; id: number; millis: number } | null>(null)
  const ref = useRef<HTMLDivElement>(null)
  const show = useCallback<ShowToast>((message, isLong = false) => {
    setToast({ message, id: Date.now(), millis: isLong ? LONG_MILLIS : SHORT_MILLIS })
  }, [])
  useEffect(() => {
    if (!toast) return
    const element = ref.current
    // Shown again so it comes above a dialog opened after it
    element?.hidePopover?.()
    element?.showPopover?.()
    const timer = window.setTimeout(() => setToast(null), toast.millis)
    return () => window.clearTimeout(timer)
  }, [toast])
  return (
    <ToastContext.Provider value={show}>
      {children}
      {toast && (
        <div ref={ref} key={toast.id} className="toast" popover="manual" role="status">
          {toast.message}
        </div>
      )}
    </ToastContext.Provider>
  )
}
