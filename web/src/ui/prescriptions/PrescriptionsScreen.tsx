import { useEffect, useRef, useState } from 'react'
import type { Prescription, PrescriptionStatus } from '../../data/models'
import { boxesTotal, canBuy, canOrder, nextBoxNumber, pillsPerRepetition, prescriptionPillsTotal } from '../../data/models'
import { useStrings } from '../../i18n/i18n'
import { useRepositoryState, useServices } from '../AppServices'
import { CardButton } from '../components/Buttons'
import { FilterSelector } from '../components/FilterSelector'
import { Icon } from '../components/Icon'
import { LinkIconButton } from '../components/LinkIconButton'
import type { ListFilter } from '../components/ListFilter'
import { RecordCard } from '../components/RecordCard'
import { StatusBadge, type StatusTone } from '../components/StatusBadge'
import { tutorialTarget } from '../components/TutorialTarget'
import { BuyRepetitionDialog } from './BuyRepetitionDialog'
import { EditPrescriptionDialog } from './EditPrescriptionDialog'
import { NewPrescriptionDialog } from './NewPrescriptionDialog'
import { type PrescriptionItem, prescriptionsUiState } from './prescriptionsState'

const STATUS_LABELS: Record<PrescriptionStatus, string> = {
  NOT_STARTED: 'prescriptions_status_not_started',
  ORDERED: 'prescriptions_status_ordered',
  IN_PROGRESS: 'prescriptions_status_in_progress',
  FINISHED: 'prescriptions_status_finished',
}

export const PRESCRIPTION_TONES: Record<PrescriptionStatus, StatusTone> = {
  NOT_STARTED: 'not-started',
  ORDERED: 'ordered',
  IN_PROGRESS: 'in-progress',
  FINISHED: 'finished',
}

/** The prescriptions tab: the list of prescriptions, with the button for a new one. */
export function PrescriptionsScreen({
  filter,
  onFilterChange,
  focusedPrescriptionId,
  onOpenBox,
}: {
  filter: ListFilter
  onFilterChange: (filter: ListFilter) => void
  focusedPrescriptionId: number | null
  onOpenBox: (boxId: number) => void
}) {
  const strings = useStrings()
  const { repository } = useServices()
  const state = prescriptionsUiState(useRepositoryState().shownData, filter)
  const [isDialogOpen, setDialogOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [buyingId, setBuyingId] = useState<number | null>(null)
  const focused = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (focusedPrescriptionId !== null) onFilterChange('ALL')
  }, [focusedPrescriptionId, onFilterChange])
  const isFocusedShown = state.items.some(it => it.prescription.id === focusedPrescriptionId)
  useEffect(() => {
    if (isFocusedShown) focused.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [focusedPrescriptionId, isFocusedShown])

  const buying = state.items.find(it => it.prescription.id === buyingId)
  const editing = state.items.find(it => it.prescription.id === editingId)
  const newPrescription = strings.t('prescriptions_new')
  return (
    <>
      <div className="tab-scroll">
        <div className="tab-list with-fab">
          <FilterSelector filter={state.filter} options={['ACTIVE', 'ALL']} onFilterChange={onFilterChange} />
          {state.items.length === 0 && (
            <p className="body-large" style={{ margin: 0 }}>
              {strings.t(state.filter === 'ACTIVE' ? 'prescriptions_empty_active' : 'prescriptions_empty_all')}
            </p>
          )}
          {state.items.map((item, index) => (
            <div
              key={item.prescription.id}
              ref={item.prescription.id === focusedPrescriptionId ? focused : undefined}
              style={{ scrollMarginTop: 16 }}
            >
              <PrescriptionCard
                item={item}
                isHighlighted={item.prescription.id === focusedPrescriptionId}
                isFirst={index === 0}
                onOrderRepetition={() => repository.orderRepetition(item.prescription.id)}
                onBuyRepetition={() => setBuyingId(item.prescription.id)}
                onEdit={() => setEditingId(item.prescription.id)}
                onOpenBox={onOpenBox}
              />
            </div>
          ))}
        </div>
      </div>
      <button type="button" className="fab" onClick={() => setDialogOpen(true)} {...tutorialTarget('NEW_PRESCRIPTION')}>
        <Icon name="ic_add" />
        <span className="label-large">{newPrescription}</span>
      </button>
      {isDialogOpen && (
        <NewPrescriptionDialog
          previous={state.previousPrescription}
          onSave={draft => {
            repository.addPrescription(draft)
            setDialogOpen(false)
          }}
          onDismiss={() => setDialogOpen(false)}
        />
      )}
      {buying && (
        <BuyRepetitionDialog
          boxCount={buying.prescription.boxesPerRepetition}
          firstBoxNumber={nextBoxNumber(buying.prescription)}
          onSave={expiryDates => {
            repository.buyRepetition(buying.prescription.id, expiryDates)
            setBuyingId(null)
          }}
          onDismiss={() => setBuyingId(null)}
        />
      )}
      {editing && (
        <EditPrescriptionDialog
          prescription={editing.prescription}
          hasBoxes={editing.boxes.length > 0}
          onSave={updated => {
            repository.editPrescription(updated)
            setEditingId(null)
          }}
          onDelete={keepBoxes => {
            repository.deletePrescription(editing.prescription.id, keepBoxes)
            setEditingId(null)
          }}
          onDismiss={() => setEditingId(null)}
        />
      )}
    </>
  )
}

function PrescriptionCard({
  item,
  isHighlighted,
  isFirst,
  onOrderRepetition,
  onBuyRepetition,
  onEdit,
  onOpenBox,
}: {
  item: PrescriptionItem
  isHighlighted: boolean
  isFirst: boolean
  onOrderRepetition: () => void
  onBuyRepetition: () => void
  onEdit: () => void
  onOpenBox: (boxId: number) => void
}) {
  const strings = useStrings()
  const p: Prescription = item.prescription
  const detail = (text: string) => <span className="body-medium text-wrap">{text}</span>
  return (
    <RecordCard
      tone={PRESCRIPTION_TONES[p.status]}
      isHighlighted={isHighlighted}
      onEdit={onEdit}
      attributes={isFirst ? tutorialTarget('FIRST_PRESCRIPTION') : undefined}
    >
      <div className="row">
        <span className="title-medium grow">{strings.t('prescriptions_prescribed_date', strings.date(p.prescribedDate))}</span>
        <StatusBadge
          status={strings.t(STATUS_LABELS[p.status])}
          amount={strings.t('prescriptions_repetitions', p.repetitionsBought, p.repetitionsTotal)}
          currentAmount={p.repetitionsBought}
        />
      </div>
      {detail(strings.t('prescriptions_medication', p.medication))}
      {detail(strings.t('prescriptions_prescribed_by', p.prescribedBy))}
      {detail(strings.t('pharmacy_label', p.pharmacy))}
      {detail(strings.t('prescriptions_per_box', p.pillsPerBox))}
      {detail(strings.t('prescriptions_per_repetition', p.boxesPerRepetition, pillsPerRepetition(p)))}
      {detail(strings.t('prescriptions_total', boxesTotal(p), prescriptionPillsTotal(p)))}
      {p.orderedDate && detail(strings.t('prescriptions_ordered_date', strings.date(p.orderedDate)))}
      {p.boughtFirstDate && detail(strings.t('prescriptions_bought_first', strings.date(p.boughtFirstDate)))}
      {p.boughtLastDate && detail(strings.t('prescriptions_bought_last', strings.date(p.boughtLastDate)))}
      {item.boxes.length > 0 && (
        <>
          <span className="label-large">{strings.t('prescriptions_linked_boxes')}</span>
          <div className="flow tight">
            {item.boxes.map(box => (
              <LinkIconButton
                key={box.id}
                icon="ic_box"
                description="link_open_box"
                onClick={() => onOpenBox(box.id)}
                label={strings.t('link_box_number', box.boxNumber)}
              />
            ))}
          </div>
        </>
      )}
      <div className="flow">
        <CardButton onClick={onOrderRepetition} disabled={!canOrder(p)}>
          {strings.t('prescriptions_order')}
        </CardButton>
        <CardButton onClick={onBuyRepetition} disabled={!canBuy(p)} isFilled>
          {strings.t('prescriptions_buy')}
        </CardButton>
      </div>
    </RecordCard>
  )
}
