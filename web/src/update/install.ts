/** The browser's install prompt, which Chromium browsers offer once a page can be installed. */
interface InstallPromptEvent extends Event {
  prompt(): Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

let installPrompt: InstallPromptEvent | null = null
const listeners = new Set<() => void>()

/** Keeps the install prompt for when the user asks to install; call before the app renders. */
export function listenForInstallPrompt() {
  window.addEventListener('beforeinstallprompt', event => {
    event.preventDefault()
    installPrompt = event as InstallPromptEvent
    listeners.forEach(listener => listener())
  })
  window.addEventListener('appinstalled', () => {
    installPrompt = null
    listeners.forEach(listener => listener())
  })
}

export const canPromptInstall = () => installPrompt !== null

export function onInstallPromptChange(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

/** Shows the browser's install prompt; whether the user installed. */
export async function promptInstall(): Promise<boolean> {
  if (!installPrompt) return false
  const prompt = installPrompt
  installPrompt = null
  await prompt.prompt()
  return (await prompt.userChoice).outcome === 'accepted'
}

/** Whether the app runs installed, in its own window. */
export const isInstalled = () =>
  window.matchMedia('(display-mode: standalone)').matches ||
  (navigator as Navigator & { standalone?: boolean }).standalone === true

/** iPhones and iPads, where Safari installs from Share → Add to Home Screen. */
export const isIos = () =>
  /iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
