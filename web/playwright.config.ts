import { defineConfig } from '@playwright/test'

const PORT = 4174

// Runs the user flows against the production build, as GitHub Pages serves it, in a phone-sized window.
// CHROMIUM_PATH picks a Chromium installed on the machine instead of Playwright's own.
export default defineConfig({
  testDir: 'e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: `http://localhost:${PORT}/medication-logger/app/`,
    viewport: { width: 412, height: 915 },
    locale: 'en-US',
    timezoneId: 'Europe/Amsterdam',
    launchOptions: { executablePath: process.env.CHROMIUM_PATH },
    trace: 'retain-on-failure',
  },
  webServer: {
    command: `npm run build && npx vite preview --port ${PORT} --strictPort`,
    url: `http://localhost:${PORT}/medication-logger/app/`,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
})
