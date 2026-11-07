// Reads the Android app's resources at build time, so the web app shows the same texts, translations, icons and
// version without keeping a copy of them: `import resources from 'virtual:android-resources'`.
import { readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'
import type { Plugin } from 'vite'

const MODULE_ID = 'virtual:android-resources'
const RESOLVED_ID = '\0' + MODULE_ID

/** A language's strings: plain ones as text, plurals as their quantity forms. */
export type StringTable = Record<string, string | Record<string, string>>

export interface AndroidResources {
  /** Strings by language tag, English under `en`. */
  strings: Record<string, StringTable>
  /** Icons by drawable name (`ic_box`), as the inner markup of a 24 × 24 SVG drawing in `currentColor`. */
  icons: Record<string, { viewBox: string; body: string }>
  versionName: string
  versionCode: number
  siteUrl: string
  feedbackUrl: string
}

// Android's string escapes and XML entities, as aapt2 reads them
function unescape(raw: string): string {
  let text = raw.trim()
  if (text.startsWith('"') && text.endsWith('"')) text = text.slice(1, -1)
  return text
    .replace(/\\'/g, "'")
    .replace(/\\"/g, '"')
    .replace(/\\n/g, '\n')
    .replace(/\\@/g, '@')
    .replace(/\\\?/g, '?')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, '&')
}

export function parseStrings(xml: string): StringTable {
  const table: StringTable = {}
  for (const match of xml.matchAll(/<string\s+name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g)) {
    table[match[1]] = unescape(match[2])
  }
  for (const match of xml.matchAll(/<plurals\s+name="([^"]+)"[^>]*>([\s\S]*?)<\/plurals>/g)) {
    const forms: Record<string, string> = {}
    for (const item of match[2].matchAll(/<item\s+quantity="([^"]+)"\s*>([\s\S]*?)<\/item>/g)) {
      forms[item[1]] = unescape(item[2])
    }
    table[match[1]] = forms
  }
  return table
}

const SVG_ATTRIBUTES: Record<string, string> = {
  pathData: 'd',
  strokeWidth: 'stroke-width',
  strokeLineCap: 'stroke-linecap',
  strokeLineJoin: 'stroke-linejoin',
  fillAlpha: 'fill-opacity',
  strokeAlpha: 'stroke-opacity',
}

interface XmlElement {
  tag: string
  attributes: Record<string, string>
  children: XmlElement[]
}

// Enough of XML for vector drawables: elements and attributes, with comments and the declaration dropped
function parseXml(xml: string): XmlElement {
  const root: XmlElement = { tag: '', attributes: {}, children: [] }
  const open = [root]
  const source = xml.replace(/<!--[\s\S]*?-->/g, '').replace(/<\?[\s\S]*?\?>/g, '')
  for (const [, closing, tag, rawAttributes, selfClosing] of source.matchAll(/<(\/?)([\w-]+)([^>]*?)(\/?)>/g)) {
    if (closing) {
      open.pop()
      continue
    }
    const attributes = Object.fromEntries([...rawAttributes.matchAll(/android:(\w+)="([^"]*)"/g)].map(([, n, v]) => [n, v]))
    const element: XmlElement = { tag, attributes, children: [] }
    open[open.length - 1].children.push(element)
    if (!selfClosing) open.push(element)
  }
  return root.children[0]
}

function pathToSvg(attributes: Record<string, string>): string {
  const svg: string[] = []
  let hasFill = false
  for (const [name, value] of Object.entries(attributes)) {
    if (name === 'fillColor') {
      hasFill = true
      svg.push('fill="currentColor"')
    } else if (name === 'strokeColor') {
      svg.push('stroke="currentColor"')
    } else if (name === 'fillType') {
      svg.push(`fill-rule="${value === 'evenOdd' ? 'evenodd' : 'nonzero'}"`)
    } else if (SVG_ATTRIBUTES[name]) {
      svg.push(`${SVG_ATTRIBUTES[name]}="${value}"`)
    }
  }
  if (!hasFill) svg.push('fill="none"')
  return `<path ${svg.join(' ')}/>`
}

// A group's transform, in the order Android applies it: scale and rotate about the pivot, then translate
function groupTransform(attributes: Record<string, string>): string {
  const number = (name: string, fallback: number) => Number(attributes[name] ?? fallback)
  const [pivotX, pivotY] = [number('pivotX', 0), number('pivotY', 0)]
  const parts = [
    `translate(${number('translateX', 0) + pivotX} ${number('translateY', 0) + pivotY})`,
    `rotate(${number('rotation', 0)})`,
    `scale(${number('scaleX', 1)} ${number('scaleY', 1)})`,
    `translate(${-pivotX} ${-pivotY})`,
  ]
  return parts.join(' ')
}

// A clip-path clips what follows it in its group
function childrenToSvg(children: XmlElement[], idPrefix: string, ids: { next: number }): string {
  let svg = ''
  for (const [index, child] of children.entries()) {
    if (child.tag === 'path') {
      svg += pathToSvg(child.attributes)
    } else if (child.tag === 'group') {
      svg += `<g transform="${groupTransform(child.attributes)}">${childrenToSvg(child.children, idPrefix, ids)}</g>`
    } else if (child.tag === 'clip-path') {
      const id = `${idPrefix}-clip-${ids.next++}`
      const rest = childrenToSvg(children.slice(index + 1), idPrefix, ids)
      return `${svg}<clipPath id="${id}"><path d="${child.attributes.pathData}"/></clipPath><g clip-path="url(#${id})">${rest}</g>`
    }
  }
  return svg
}

/**
 * Converts a vector drawable to SVG markup, its fill and stroke colors becoming `currentColor`, with its groups'
 * transforms and clip paths. Clip ids start with [name], so different icons on a page don't share them.
 */
export function vectorToSvg(xml: string, name = 'icon'): { viewBox: string; body: string } {
  const vector = parseXml(xml)
  const width = vector.attributes.viewportWidth ?? '24'
  const height = vector.attributes.viewportHeight ?? '24'
  return { viewBox: `0 0 ${width} ${height}`, body: childrenToSvg(vector.children, name, { next: 0 }) }
}

export function loadAndroidResources(root: string): AndroidResources {
  const res = join(root, 'app/src/main/res')
  const strings: Record<string, StringTable> = {}
  for (const folder of readdirSync(res)) {
    const match = /^values(?:-([a-z]{2}))?$/.exec(folder)
    if (!match) continue
    try {
      strings[match[1] ?? 'en'] = parseStrings(readFileSync(join(res, folder, 'strings.xml'), 'utf8'))
    } catch {
      // A values folder without strings, such as values-night
    }
  }
  const icons: AndroidResources['icons'] = {}
  for (const file of readdirSync(join(res, 'drawable'))) {
    if (file.startsWith('ic_') && file.endsWith('.xml')) {
      const name = file.slice(0, -4)
      icons[name] = vectorToSvg(readFileSync(join(res, 'drawable', file), 'utf8'), name)
    }
  }
  const gradle = readFileSync(join(root, 'app/build.gradle.kts'), 'utf8')
  const field = (name: string) => new RegExp(`"${name}",\\s*"\\\\"([^"\\\\]*)\\\\""`).exec(gradle)?.[1] ?? ''
  return {
    strings,
    icons,
    versionName: /versionName = "([^"]+)"/.exec(gradle)?.[1] ?? '0',
    versionCode: Number(/versionCode = (\d+)/.exec(gradle)?.[1] ?? 0),
    siteUrl: field('SITE_URL'),
    feedbackUrl: field('FEEDBACK_URL'),
  }
}

/** Serves the Android resources found under [root] as `virtual:android-resources`, reloading when they change. */
export function androidResources(root: string): Plugin {
  return {
    name: 'android-resources',
    resolveId: id => (id === MODULE_ID ? RESOLVED_ID : undefined),
    load(id) {
      if (id !== RESOLVED_ID) return undefined
      const resources = loadAndroidResources(root)
      this.addWatchFile(join(root, 'app/build.gradle.kts'))
      return `export default ${JSON.stringify(resources)}`
    },
    configureServer(server) {
      server.watcher.add(join(root, 'app/src/main/res'))
      server.watcher.on('change', file => {
        if (!file.includes('app/src/main/res') && !file.endsWith('build.gradle.kts')) return
        const module = server.moduleGraph.getModuleById(RESOLVED_ID)
        if (module) server.moduleGraph.invalidateModule(module)
        server.ws.send({ type: 'full-reload' })
      })
    },
  }
}
