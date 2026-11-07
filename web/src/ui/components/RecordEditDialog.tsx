import { type ReactNode, useState } from 'react'
import { useStrings } from '../../i18n/i18n'
import { TextButton } from './Buttons'
import { Dialog } from './Dialog'

/**
 * Edits a record's [children] fields, with a delete button that asks for confirmation, saying [deleteMessage].
 * With [keepChildrenLabel], the confirmation also offers deleting only the record.
 */
export function RecordEditDialog({
  title,
  canSave,
  deleteMessage,
  onSave,
  onDelete,
  onDismiss,
  keepChildrenLabel,
  onDeleteKeepingChildren,
  children,
}: {
  title: string
  canSave: boolean
  deleteMessage: string
  onSave: () => void
  onDelete: () => void
  onDismiss: () => void
  keepChildrenLabel?: string
  onDeleteKeepingChildren?: () => void
  children: ReactNode
}) {
  const strings = useStrings()
  const [isConfirmingDelete, setConfirmingDelete] = useState(false)
  if (isConfirmingDelete) {
    return (
      <Dialog
        key="delete"
        title={strings.t('edit_delete_title')}
        onDismiss={() => setConfirmingDelete(false)}
        buttons={
          <>
            <TextButton onClick={() => setConfirmingDelete(false)}>{strings.t('edit_cancel')}</TextButton>
            {keepChildrenLabel && <TextButton onClick={onDeleteKeepingChildren}>{keepChildrenLabel}</TextButton>}
            <TextButton isDanger onClick={onDelete}>
              {strings.t(keepChildrenLabel ? 'edit_delete_all' : 'edit_delete')}
            </TextButton>
          </>
        }
      >
        {deleteMessage}
      </Dialog>
    )
  }
  return (
    <Dialog
      key="edit"
      title={title}
      onDismiss={onDismiss}
      buttons={
        <>
          <TextButton onClick={onDismiss}>{strings.t('edit_cancel')}</TextButton>
          <TextButton onClick={onSave} disabled={!canSave}>
            {strings.t('edit_save')}
          </TextButton>
        </>
      }
    >
      {children}
      <TextButton isDanger onClick={() => setConfirmingDelete(true)}>
        {strings.t('edit_delete')}
      </TextButton>
    </Dialog>
  )
}
