import type { ComponentProps } from 'react'

type Props = ComponentProps<'button'>

const classes = (...names: (string | false | undefined)[]) => names.filter(Boolean).join(' ')

export const TextButton = ({ className, isDanger, ...props }: Props & { isDanger?: boolean }) => (
  <button type="button" {...props} className={classes('button button-text', isDanger && 'danger', className)} />
)

export const FilledButton = ({ className, ...props }: Props) => (
  <button type="button" {...props} className={classes('button button-filled', className)} />
)

export const OutlinedButton = ({ className, ...props }: Props) => (
  <button type="button" {...props} className={classes('button button-outlined', className)} />
)

/** A compact button on a record card, outlined unless [isFilled] marks the likely next action. */
export const CardButton = ({
  className,
  isFilled = false,
  isIconOnly = false,
  isSmallShape = false,
  ...props
}: Props & { isFilled?: boolean; isIconOnly?: boolean; isSmallShape?: boolean }) => (
  <button
    type="button"
    {...props}
    className={classes(
      'button card-button',
      isFilled ? 'button-filled' : 'button-outlined',
      isIconOnly && 'icon-only',
      isSmallShape && 'small-shape',
      className,
    )}
  />
)

export const IconButton = ({ className, ...props }: Props) => (
  <button type="button" {...props} className={classes('icon-button', className)} />
)
