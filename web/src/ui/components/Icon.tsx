import resources from 'virtual:android-resources'

// Material icons the Android app gets from Compose itself rather than from its drawables
const MATERIAL_ICONS: Record<string, { viewBox: string; body: string }> = {
  check: { viewBox: '0 0 24 24', body: '<path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>' },
  arrow_drop_down: { viewBox: '0 0 24 24', body: '<path fill="currentColor" d="M7 10l5 5 5-5z"/>' },
}

/** One of the Android app's vector drawables (`ic_box`), drawn in the current text color. */
export function Icon({ name, size = 24, label, className }: { name: string; size?: number; label?: string; className?: string }) {
  const icon = resources.icons[name] ?? MATERIAL_ICONS[name]
  return (
    <svg
      className={`icon ${className ?? ''}`}
      width={size}
      height={size}
      viewBox={icon?.viewBox ?? '0 0 24 24'}
      role={label ? 'img' : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
      dangerouslySetInnerHTML={{ __html: icon?.body ?? '' }}
    />
  )
}
