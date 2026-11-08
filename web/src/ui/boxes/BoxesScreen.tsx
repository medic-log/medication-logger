import { useEffect, useRef, useState } from 'react'
import { type BoxStatus, type Refill, boxStatus, canRefill, canSaveAsExtra } from '../../data/models'
import { type Strings, useStrings } from '../../i18n/i18n'
import { useNow, useRepositoryState, useServices } from '../AppServices'
import { CardButton } from '../components/Buttons'
import { FilterSelector } from '../components/FilterSelector'
import { LinkIconButton } from '../components/LinkIconButton'
import type { ListFilter } from '../components/ListFilter'
import { RecordCard } from '../components/RecordCard'
import { StatusBadge, type StatusTone } from '../components/StatusBadge'
import { TextField } from '../components/TextField'
import { tutorialTarget } from '../components/TutorialTarget'
import { type BoxItem, boxesUiState } from './boxesState'
import { EditBoxDialog } from './EditBoxDialog'

const STATUS_LABELS: Record<BoxStatus, string> = {
  NOT_STARTED: 'boxes_status_not_started',
  IN_PROGRESS: 'boxes_status_in_progress',
  FINISHED: 'boxes_status_finished',
  EXTRA: 'boxes_status_extra',
}

const TONES: Record<BoxStatus, StatusTone> = {
  NOT_STARTED: 'not-started',
  IN_PROGRESS: 'in-progress',
  FINISHED: 'finished',
  EXTRA: 'extra',
}

const EMPTY_TEXTS: Record<ListFilter, string> = {
  ACTIVE: 'boxes_empty_active',
  ALL: 'boxes_empty_all',
  EXTRA: 'boxes_empty_extra',
}

/** The boxes tab: the list of boxes with their refill actions. */
export function BoxesScreen({
  filter,
  onFilterChange,
  focusedBoxId,
  onOpenPrescription,
}: {
  filter: ListFilter
  onFilterChange: (filter: ListFilter) => void
  focusedBoxId: number | null
  onOpenPrescription: (prescriptionId: number) => void
}) {
  const strings = useStrings()
  const { repository } = useServices()
  const state = boxesUiState(useRepositoryState().shownData, filter, useNow())
  const [editingId, setEditingId] = useState<number | null>(null)
  const focused = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (focusedBoxId !== null) onFilterChange('ALL')
  }, [focusedBoxId, onFilterChange])
  const isFocusedShown = state.items.some(it => it.box.id === focusedBoxId)
  useEffect(() => {
    if (isFocusedShown) focused.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [focusedBoxId, isFocusedShown])

  const editing = state.items.find(it => it.box.id === editingId)
  return (
    <div className="tab-scroll">
      <div className="tab-list">
        <FilterSelector filter={state.filter} options={['ACTIVE', 'ALL', 'EXTRA']} onFilterChange={onFilterChange} />
        {state.pendingRefillAmount > 0 && (
          <div className="notice pending body-medium">
            {strings.plural('boxes_pending_notice', state.pendingRefillAmount, state.pendingRefillAmount)}
          </div>
        )}
        {state.items.length === 0 && (
          <p className="body-large" style={{ margin: 0 }}>
            {strings.t(EMPTY_TEXTS[state.filter])}
          </p>
        )}
        {state.items.map((item, index) => (
          <div key={item.box.id} ref={item.box.id === focusedBoxId ? focused : undefined} style={{ scrollMarginTop: 16 }}>
            <BoxCard
              item={item}
              pendingRefillAmount={state.pendingRefillAmount}
              isHighlighted={item.box.id === focusedBoxId}
              isFirst={index === 0}
              onRefillWeekly={() => repository.refillWeekly(item.box.id)}
              onRefillPartial={() => repository.refillPartial(item.box.id)}
              onSaveAsExtra={() => repository.saveAsExtra(item.box.id)}
              onBackToInventory={() => repository.backToInventory(item.box.id)}
              onNotesChange={notes => repository.setBoxNotes(item.box.id, notes)}
              onEdit={() => setEditingId(item.box.id)}
              onOpenPrescription={
                item.box.prescriptionId !== null ? () => onOpenPrescription(item.box.prescriptionId!) : null
              }
            />
          </div>
        ))}
      </div>
      {editing && (
        <EditBoxDialog
          box={editing.box}
          hasRefills={editing.refills.length > 0}
          onSave={updated => {
            repository.editBox(updated)
            setEditingId(null)
          }}
          onDelete={keepRefills => {
            repository.deleteBox(editing.box.id, keepRefills)
            setEditingId(null)
          }}
          onDismiss={() => setEditingId(null)}
        />
      )}
    </div>
  )
}

function BoxCard({
  item,
  pendingRefillAmount,
  isHighlighted,
  isFirst,
  onRefillWeekly,
  onRefillPartial,
  onSaveAsExtra,
  onBackToInventory,
  onNotesChange,
  onEdit,
  onOpenPrescription,
}: {
  item: BoxItem
  pendingRefillAmount: number
  isHighlighted: boolean
  isFirst: boolean
  onRefillWeekly: () => void
  onRefillPartial: () => void
  onSaveAsExtra: () => void
  onBackToInventory: () => void
  onNotesChange: (notes: string) => void
  onEdit: () => void
  onOpenPrescription: (() => void) | null
}) {
  const strings = useStrings()
  const box = item.box
  const status = boxStatus(box)
  // The action the user most likely needs next gets the filled button
  const isPartialHighlighted = pendingRefillAmount > 0 && canRefill(box)
  return (
    <RecordCard
      tone={TONES[status]}
      isHighlighted={isHighlighted}
      onEdit={onEdit}
      attributes={isFirst ? tutorialTarget('FIRST_BOX') : undefined}
    >
      <div className="row">
        <div className="column grow">
          <span className="title-medium">{strings.boxName(box)}</span>
          {box.openDate && <span className="body-medium">{strings.t('boxes_opened', strings.date(box.openDate))}</span>}
          {box.expiryDate && <span className="body-medium">{strings.t('boxes_expires', strings.month(box.expiryDate))}</span>}
        </div>
        <StatusBadge
          status={strings.t(STATUS_LABELS[status])}
          amount={strings.plural('boxes_pills_left', box.pillsTotal, box.pillsLeft, box.pillsTotal)}
          currentAmount={box.pillsLeft}
        />
      </div>
      {box.isExtra && <BoxNotesField notes={box.notes} onNotesChange={onNotesChange} />}
      <div className="row bottom">
        <div className="column grow">
          {item.refills.length > 0 && (
            <>
              <span className="label-large" style={box.isExtra ? { paddingTop: 4 } : undefined}>
                {strings.t('boxes_refills_title')}
              </span>
              {item.refills.map(refill => (
                <span key={refill.id} className="body-small">
                  {refillSummary(refill, strings)}
                </span>
              ))}
            </>
          )}
        </div>
        <LinkIconButton
          icon="ic_prescription"
          description={onOpenPrescription ? 'link_open_prescription' : 'link_prescription_deleted'}
          onClick={onOpenPrescription}
        />
      </div>
      <div className="flow">
        {isPartialHighlighted ? (
          <>
            <CardButton onClick={onRefillPartial} isFilled>
              {strings.plural('boxes_refill_partial_pending', pendingRefillAmount, pendingRefillAmount)}
            </CardButton>
            <CardButton onClick={onRefillWeekly}>{strings.t('boxes_refill_weekly')}</CardButton>
          </>
        ) : (
          canRefill(box) && (
            <CardButton onClick={onRefillWeekly} isFilled>
              {strings.t('boxes_refill_weekly')}
            </CardButton>
          )
        )}
        {box.isExtra ? (
          <CardButton onClick={onBackToInventory}>{strings.t('boxes_back_to_inventory')}</CardButton>
        ) : (
          <CardButton onClick={onSaveAsExtra} disabled={!canSaveAsExtra(box)}>
            {strings.t('boxes_save_as_extra')}
          </CardButton>
        )}
      </div>
    </RecordCard>
  )
}

/** Notes typed on the card, saved when the field loses focus or Enter is pressed. */
function BoxNotesField({ notes, onNotesChange }: { notes: string; onNotesChange: (notes: string) => void }) {
  const strings = useStrings()
  const [text, setText] = useState(notes)
  // Changes from elsewhere, such as an import, replace what's shown
  useEffect(() => setText(notes), [notes])
  const save = () => {
    if (text.trim() !== notes) onNotesChange(text)
  }
  return (
    <TextField
      value={text}
      onChange={setText}
      label={strings.t('boxes_notes_label')}
      className="on-card"
      autoCapitalize="sentences"
      onBlur={save}
      onEnter={() => (document.activeElement as HTMLElement | null)?.blur()}
    />
  )
}

function refillSummary(refill: Refill, strings: Strings): string {
  const amounts = strings.plural('refills_amount', refill.amount, refill.amount, refill.amountRemaining)
  const line = `${strings.date(refill.date)}: ${amounts}`
  return refill.isPartial ? strings.t('refills_partial_suffix', line) : line
}
