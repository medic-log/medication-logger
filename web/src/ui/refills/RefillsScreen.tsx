import { useState } from 'react'
import type { MedicationMiss, MissKind } from '../../data/models'
import { isMissed } from '../../data/models'
import { localDateTimeOf, toLocalTime } from '../../data/time'
import { useStrings } from '../../i18n/i18n'
import { useNow, useRepositoryState, useServices } from '../AppServices'
import { OutlinedButton } from '../components/Buttons'
import { LinkIconButton } from '../components/LinkIconButton'
import { RecordCard } from '../components/RecordCard'
import { BoldNumber } from '../components/StatusBadge'
import { TimePickerDialog } from '../components/TimeSelector'
import { tutorialTarget } from '../components/TutorialTarget'
import { EditMissDialog } from './EditMissDialog'
import { EditRefillDialog } from './EditRefillDialog'
import { type MissEntry, type RefillEntry, type RefillGroupEntry, type RefillLogEntry, refillsUiState } from './refillsState'

const MISS_LABELS: Record<MissKind, string> = { MISSED: 'refills_missed', DELAYED: 'refills_delayed' }

/** The refills tab: the log of refills, misses and delays. */
export function RefillsScreen({
  onOpenPrescription,
  onOpenBox,
}: {
  onOpenPrescription: (prescriptionId: number) => void
  onOpenBox: (boxId: number) => void
}) {
  const strings = useStrings()
  const { repository } = useServices()
  const state = refillsUiState(useRepositoryState().shownData, useNow())
  const [editingKey, setEditingKey] = useState<string | null>(null)
  const [isPickingDelayTime, setPickingDelayTime] = useState(false)
  const lotMessage = strings.plural('refills_lot_pills', state.lotPills, state.lotPills)

  const allEntries = state.entries.flatMap(it => (it.type === 'group' ? it.members : [it]))
  const editing = allEntries.find(it => it.key === editingKey)
  const links = { onOpenPrescription, onOpenBox, onEdit: setEditingKey }
  return (
    <div className="tab-scroll">
      <div className="tab-list">
        <div className="column" style={{ alignItems: 'center', gap: 8 }} {...tutorialTarget('DOSE_LOG')}>
          <div className="dose-buttons">
            <OutlinedButton onClick={() => repository.logMiss('MISSED')}>{strings.t('refills_log_missed')}</OutlinedButton>
            <OutlinedButton onClick={() => setPickingDelayTime(true)}>{strings.t('refills_log_delayed')}</OutlinedButton>
          </div>
          <span className="body-medium">
            <BoldNumber text={lotMessage} number={state.lotPills} />
          </span>
        </div>
        {state.entries.length === 0 && (
          <p className="body-large" style={{ margin: 0 }}>
            {strings.t('refills_empty')}
          </p>
        )}
        {state.entries.map((entry, index) => (
          <LogEntry key={entry.key} entry={entry} isFirst={index === 0} {...links} />
        ))}
      </div>
      {isPickingDelayTime && (
        <TimePickerDialog
          time={toLocalTime(localDateTimeOf(new Date()))}
          title={strings.t('refills_delayed_time_dialog_title')}
          onConfirm={time => {
            repository.logMiss('DELAYED', time)
            setPickingDelayTime(false)
          }}
          onDismiss={() => setPickingDelayTime(false)}
        />
      )}
      {editing?.type === 'refill' && (
        <EditRefillDialog
          refill={editing.refill}
          maxAmount={editing.maxAmount}
          leftRange={editing.leftRange}
          onSave={updated => {
            repository.editRefill(updated)
            setEditingKey(null)
          }}
          onDelete={() => {
            repository.deleteRefill(editing.refill.id)
            setEditingKey(null)
          }}
          onDismiss={() => setEditingKey(null)}
        />
      )}
      {editing?.type === 'miss' && (
        <EditMissDialog
          miss={editing.miss}
          onSave={updated => {
            repository.editMiss(updated)
            setEditingKey(null)
          }}
          onDelete={() => {
            repository.deleteMiss(editing.miss.id)
            setEditingKey(null)
          }}
          onDismiss={() => setEditingKey(null)}
        />
      )}
    </div>
  )
}

interface Links {
  onOpenPrescription: (prescriptionId: number) => void
  onOpenBox: (boxId: number) => void
  onEdit: (key: string) => void
}

function LogEntry({ entry, isFirst, ...links }: { entry: RefillLogEntry; isFirst: boolean } & Links) {
  const target = isFirst ? tutorialTarget('FIRST_REFILL') : undefined
  switch (entry.type) {
    case 'refill':
      return <RefillCard entry={entry} attributes={target} {...links} />
    case 'group':
      return <RefillGroupCard group={entry} attributes={target} {...links} />
    case 'miss':
      return <MissCard miss={entry.miss} attributes={target} onEdit={() => links.onEdit(entry.key)} />
  }
}

// Pasted together, sandwich style: only the outer corners of the stack are round
function RefillGroupCard({ group, attributes, ...links }: { group: RefillGroupEntry; attributes?: Record<string, string> } & Links) {
  return (
    <div className="stack" {...attributes}>
      {group.members.map((member: RefillEntry | MissEntry, index) => (
        <GroupMember key={member.key} member={member} isAfterFirst={index > 0} {...links} />
      ))}
    </div>
  )
}

function GroupMember({ member, isAfterFirst, ...links }: { member: RefillEntry | MissEntry; isAfterFirst: boolean } & Links) {
  return (
    <>
      {isAfterFirst && <hr />}
      {member.type === 'refill' ? (
        <RefillCard entry={member} {...links} />
      ) : (
        <MissCard miss={member.miss} onEdit={() => links.onEdit(member.key)} isCounted={isMissed(member.miss)} />
      )}
    </>
  )
}

function RefillCard({
  entry,
  onOpenPrescription,
  onOpenBox,
  onEdit,
  attributes,
}: { entry: RefillEntry; attributes?: Record<string, string> } & Links) {
  const strings = useStrings()
  const refill = entry.refill
  const openPrescription = refill.prescriptionId !== null ? () => onOpenPrescription(refill.prescriptionId!) : null
  const openBox = refill.boxId !== null ? () => onOpenBox(refill.boxId!) : null
  return (
    <RecordCard onEdit={() => onEdit(entry.key)} attributes={attributes}>
      <div className="row center">
        <span className="title-medium grow">{refill.date}</span>
        {refill.isPartial && <span className="badge-partial label-medium">{strings.t('refills_partial')}</span>}
      </div>
      <div className="row center" style={{ gap: 4 }}>
        <div className="column grow">
          <span className="body-medium">{strings.plural('refills_pills', refill.amount, refill.amount)}</span>
          {openBox && <span className="body-small">{strings.t('refills_box', entry.boxName, refill.amountRemaining)}</span>}
        </div>
        <LinkIconButton
          icon="ic_prescription"
          description={openPrescription ? 'link_open_prescription' : 'link_prescription_deleted'}
          onClick={openPrescription}
        />
        <LinkIconButton
          icon="ic_box"
          description={openBox ? 'link_open_box' : 'link_box_deleted'}
          onClick={openBox}
          label={entry.boxNumber !== null ? strings.t('link_box_number', entry.boxNumber) : undefined}
        />
      </div>
    </RecordCard>
  )
}

function MissCard({
  miss,
  onEdit,
  isCounted = false,
  attributes,
}: {
  miss: MedicationMiss
  onEdit: () => void
  isCounted?: boolean
  attributes?: Record<string, string>
}) {
  const strings = useStrings()
  return (
    <RecordCard onEdit={onEdit} attributes={attributes}>
      <span className="title-medium">{miss.date}</span>
      <span className="body-medium">{strings.t(MISS_LABELS[miss.kind])}</span>
      {miss.time && <span className="body-small">{strings.t('refills_delayed_time', miss.time)}</span>}
      {isCounted && <span className="body-small">{strings.t('refills_missed_pill')}</span>}
    </RecordCard>
  )
}
