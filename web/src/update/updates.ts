import resources from 'virtual:android-resources'
import { registerSW } from 'virtual:pwa-register'

/** The version the deployed app is, from the version.json the build writes next to it. */
interface DeployedVersion {
  versionCode: number
  versionName: string
}

// How long to wait for the new version's service worker to install before reloading anyway
const INSTALL_TIMEOUT_MILLIS = 15_000

/**
 * The web app's updates, in place of the Android app's APK download: a new deploy is found by comparing
 * version.json's versionCode with this one's, as the Android app does with latest.json, and applied by
 * switching to its service worker and reloading.
 */
export class Updates {
  private registration: ServiceWorkerRegistration | undefined
  private isWaiting = false
  private onWaiting: (() => void) | null = null
  private readonly updateServiceWorker = registerSW({
    onNeedRefresh: () => {
      this.isWaiting = true
      this.onWaiting?.()
    },
    onRegisteredSW: (_url, registration) => {
      this.registration = registration
    },
  })

  /** The newer version deployed, null when this is the latest, or 'failed' when it can't be checked. */
  async findNewer(): Promise<DeployedVersion | null | 'failed'> {
    try {
      const response = await fetch(`${import.meta.env.BASE_URL}version.json`, { cache: 'no-store' })
      if (!response.ok) return 'failed'
      const latest = (await response.json()) as DeployedVersion
      return latest.versionCode > resources.versionCode ? latest : null
    } catch {
      return 'failed'
    }
  }

  /** Switches to the newest version, reloading the app; the records and settings stay where they are. */
  async apply(): Promise<void> {
    if (!this.isWaiting) {
      const waiting = new Promise<void>(resolve => {
        this.onWaiting = resolve
        window.setTimeout(resolve, INSTALL_TIMEOUT_MILLIS)
      })
      await this.registration?.update().catch(() => undefined)
      if (this.registration) await waiting
    }
    if (this.isWaiting) await this.updateServiceWorker(true)
    else window.location.reload()
  }
}
