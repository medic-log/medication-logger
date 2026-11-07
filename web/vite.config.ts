/// <reference types="vitest/config" />
import { fileURLToPath } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'
import { androidResources, loadAndroidResources } from './android-resources.ts'

// The repository root, where the Android app's resources are
const root = fileURLToPath(new URL('..', import.meta.url))

export default defineConfig({
  // Where GitHub Pages serves it: https://medic-log.github.io/medication-logger/app/
  base: '/medication-logger/app/',
  plugins: [
    react(),
    androidResources(root),
    // The deployed version, which Update and the start-up check compare with this one's
    {
      name: 'version-json',
      generateBundle() {
        const { versionCode, versionName } = loadAndroidResources(root)
        this.emitFile({ type: 'asset', fileName: 'version.json', source: JSON.stringify({ versionCode, versionName }) })
      },
    },
    VitePWA({
      // An update waits until the user accepts it, like the Android app's update prompt
      registerType: 'prompt',
      includeAssets: ['icon.svg', 'apple-touch-icon.png'],
      manifest: {
        id: '/medication-logger/app/',
        name: 'Medication logger',
        short_name: 'Medication logger',
        description: 'Track your prescriptions, pill boxes and refills, with reminders.',
        start_url: '.',
        scope: '.',
        display: 'standalone',
        orientation: 'any',
        background_color: '#1a1110',
        theme_color: '#b84c42',
        icons: [
          { src: 'icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: 'icon-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
          { src: 'icon.svg', sizes: 'any', type: 'image/svg+xml' },
        ],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,png,webmanifest}'],
        navigateFallback: 'index.html',
        cleanupOutdatedCaches: true,
        // Opens the app when a reminder is tapped
        importScripts: ['sw-notifications.js'],
      },
    }),
  ],
  test: {
    include: ['src/**/*.test.ts'],
  },
})
