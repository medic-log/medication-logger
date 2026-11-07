// Draws the web app's icons from the Android launcher icon (ic_launcher_foreground on ic_launcher_background), so the
// two apps look the same on a home screen. Run after changing the launcher icon: `npm run icons`.
// Uses Playwright's Chromium, or the one at CHROMIUM_PATH.
import { readFileSync, writeFileSync } from 'node:fs'
import { chromium } from '@playwright/test'

const res = new URL('../../app/src/main/res/', import.meta.url)
const background = /name="ic_launcher_background">(#[0-9A-Fa-f]{6})</.exec(readFileSync(new URL('values/colors.xml', res), 'utf8'))[1]
const foreground = readFileSync(new URL('drawable/ic_launcher_foreground.xml', res), 'utf8')
const paths = [...foreground.matchAll(/<path\b([\s\S]*?)\/>/g)]
  .map(([, attributes]) => {
    const d = /android:pathData="([^"]+)"/.exec(attributes)[1]
    const stroke = /android:strokeWidth="([\d.]+)"/.exec(attributes)?.[1]
    return stroke
      ? `<path d="${d}" fill="none" stroke="#ffffff" stroke-width="${stroke}"/>`
      : `<path d="${d}" fill="#ffffff"/>`
  })
  .join('')

// An adaptive icon shows the middle 72 of its 108 units; a maskable one keeps a little more margin
const squircle = 'M54,0 C14,0 0,14 0,54 C0,94 14,108 54,108 C94,108 108,94 108,54 C108,14 94,0 54,0 Z'
const svg = (viewBox, shape) =>
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${viewBox}">${shape}${paths}</svg>`
const rounded = svg('0 0 108 108', `<path d="${squircle}" fill="${background}"/>`).replace(
  paths,
  `<g transform="translate(54 54) scale(1.4) translate(-54 -54)">${paths}</g>`,
)
const fullBleed = (viewBox) => svg(viewBox, `<rect x="-50" y="-50" width="208" height="208" fill="${background}"/>`)

writeFileSync(new URL('../public/icon.svg', import.meta.url), rounded + '\n')
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH })
const page = await browser.newPage()
async function render(markup, size, file) {
  await page.setViewportSize({ width: size, height: size })
  await page.setContent(`<style>*{margin:0}svg{display:block;width:${size}px;height:${size}px}</style>${markup}`)
  await page.screenshot({ path: new URL(`../public/${file}`, import.meta.url).pathname, omitBackground: true })
}
await render(rounded, 192, 'icon-192.png')
await render(rounded, 512, 'icon-512.png')
await render(fullBleed('9 9 90 90'), 512, 'icon-maskable-512.png')
await render(fullBleed('18 18 72 72'), 180, 'apple-touch-icon.png')
await browser.close()
