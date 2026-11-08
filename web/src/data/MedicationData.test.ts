// Ported from app/src/test/.../data/MedicationDataTest.kt
import { describe, expect, test } from 'vitest'
import { D, draftOf } from './testing'
import { boxStatus, boxesTotal, isPrescriptionActive, pillsPerRepetition, prescriptionPillsTotal } from './models'
import { atTime, plusDays, plusDaysTime, plusWeeks } from './time'

const day1 = '2026-01-05'
const day2 = '2026-02-05'
// An hour after the default Sunday refill reminder, so a full week of doses is ahead
const sunday = (week: number) => atTime(plusWeeks('2026-01-04', week), '21:00')
const draft = (pillsPerBox = 30, boxesPerRepetition = 3, repetitionsTotal = 2) =>
  draftOf('Test medication', 'Dr Test', 'Test Pharmacy', pillsPerBox, boxesPerRepetition, repetitionsTotal)
const prescribed = new D().addPrescription(draft(), day1)
const evening = (date: string) => atTime(date, '21:00')

describe('MedicationData', () => {
  test('new prescription is dated today, not started, with computed totals', () => {
    const [prescription] = prescribed.prescriptions
    expect(prescription.prescribedDate).toBe(day1)
    expect(prescription.status).toBe('NOT_STARTED')
    expect(pillsPerRepetition(prescription)).toBe(90)
    expect(boxesTotal(prescription)).toBe(6)
    expect(prescriptionPillsTotal(prescription)).toBe(180)
  })

  test('ordering sets the status without buying anything', () => {
    const ordered = prescribed.orderRepetition(1, day1)
    const [prescription] = ordered.prescriptions
    expect(prescription.status).toBe('ORDERED')
    expect(prescription.orderedDate).toBe(day1)
    expect(prescription.repetitionsBought).toBe(0)
    expect(ordered.boxes).toEqual([])
  })

  test('buying numbers boxes across repetitions and links them', () => {
    const first = prescribed.buyRepetition(1, day1)
    expect(first.boxes.map(it => it.boxNumber)).toEqual([1, 2, 3])
    const second = first.buyRepetition(1, day2)
    expect(second.boxes.map(it => it.boxNumber)).toEqual([1, 2, 3, 4, 5, 6])
    expect(second.boxes.every(it => it.prescriptionId === 1)).toBe(true)
    expect(second.boxes.at(-1)!.pillsLeft).toBe(30)
    expect(second.boxes[3].boughtDate).toBe(day2)
  })

  test('buying updates dates, repetitions and status until finished', () => {
    const first = prescribed.orderRepetition(1, day1).buyRepetition(1, day1)
    expect(first.prescriptions[0]).toMatchObject({
      status: 'IN_PROGRESS', repetitionsBought: 1, boughtFirstDate: day1, boughtLastDate: day1, orderedDate: null,
    })
    const second = first.buyRepetition(1, day2)
    expect(second.prescriptions[0]).toMatchObject({
      status: 'FINISHED', repetitionsBought: 2, boughtFirstDate: day1, boughtLastDate: day2,
    })
    expect(isPrescriptionActive(second.prescriptions[0])).toBe(false)
  })

  test("a finished prescription can't be ordered or bought again", () => {
    const finished = prescribed.buyRepetition(1, day1).buyRepetition(1, day1)
    expect(finished.buyRepetition(1, day2).data).toEqual(finished.data)
    expect(finished.orderRepetition(1, day2).data).toEqual(finished.data)
  })

  test('refills follow the example in the concept', () => {
    let data = new D()
      .addPrescription(draft(25, 1, 1), day1)
      .addPrescription(draft(30, 1, 1), day1)
      .buyRepetition(1, day1)
      .buyRepetition(2, day1)
    for (let week = 0; week < 4; week++) data = data.refillWeekly(1, sunday(week))
    expect(data.pendingRefillAmount(sunday(3))).toBe(3)
    data = data.refillPartial(2, sunday(3))
    expect(data.pendingRefillAmount(sunday(3))).toBe(0)
    data = data.refillWeekly(2, sunday(4))
    expect(data.refills.map(it => [it.amount, it.amountRemaining, it.isPartial])).toEqual([
      [7, 18, false], [7, 11, false], [7, 4, false], [4, 0, true], [3, 27, true], [7, 20, false],
    ])
    expect(data.refills.map(it => it.boxId)).toEqual([1, 1, 1, 1, 2, 2])
    expect(data.refills.map(it => it.prescriptionId)).toEqual([1, 1, 1, 1, 2, 2])
    expect(data.refills.at(-1)!.date).toBe(plusWeeks('2026-01-04', 4))
  })

  test('a partial refill from a nearly empty box leaves the rest pending', () => {
    let data = new D().addPrescription(draft(4, 2, 2), day1).buyRepetition(1, day1).buyRepetition(1, day1)
    data = data.refillWeekly(1, sunday(0)).refillPartial(2, sunday(0))
    expect(data.pendingRefillAmount(sunday(0))).toBe(0)
    // Box 2 has only one pill left, and box 3 four
    data = data.refillWeekly(2, sunday(1)).refillPartial(3, sunday(1))
    expect(data.pendingRefillAmount(sunday(1))).toBe(2)
    data = data.refillPartial(4, sunday(1))
    expect(data.pendingRefillAmount(sunday(1))).toBe(0)
    expect(data.refills.at(-1)!.amount).toBe(2)
  })

  test('partial refill does nothing when the last refill is complete', () => {
    const data = prescribed.buyRepetition(1, day1).refillWeekly(1, evening(day1))
    expect(data.refillPartial(1, evening(day1)).data).toBe(data.data)
  })

  test('the weekly lot loses a pill at each pill time and gets one back for a missed dose', () => {
    const refilled = prescribed.buyRepetition(1, day1).refillWeekly(1, sunday(0))
    const wednesday = plusDaysTime(sunday(0), 3)
    expect(refilled.lotPills(wednesday)).toBe(4)
    expect(refilled.pendingRefillAmount(wednesday)).toBe(0)
    const missed = refilled.logMiss('MISSED', wednesday)
    expect(missed.lotPills(wednesday)).toBe(5)
    expect(missed.lotPills(plusDaysTime(wednesday, 1))).toBe(4)
    expect(missed.lotPills(sunday(2))).toBe(0)
  })

  test('a partial refill tops up the lot as counted now, after a missed dose', () => {
    let data = new D()
      .addPrescription(draft(5, 2, 1), day1)
      .buyRepetition(1, day1)
      .refillWeekly(1, sunday(0))
      .logMiss('MISSED', plusDaysTime(sunday(0), 1))
    expect(data.pendingRefillAmount(plusDaysTime(sunday(0), 1))).toBe(1)
    data = data.refillPartial(2, plusDaysTime(sunday(0), 1))
    expect(data.refills.at(-1)!.amount).toBe(1)
  })

  test('editing or deleting a refill or miss changes the lot', () => {
    const data = prescribed.buyRepetition(1, day1).refillWeekly(1, sunday(0)).refillWeekly(1, sunday(1))
      .logMiss('MISSED', sunday(1))
    expect(data.deleteMiss(1).lotPills(sunday(1))).toBe(7)
    expect(data.deleteRefill(2).lotPills(sunday(1))).toBe(1)
    const delayed = data.editMiss({ ...data.misses[0], kind: 'DELAYED' })
    expect(delayed.lotPills(sunday(1))).toBe(7)
  })

  test('the weekly lot is 0 without refills', () => {
    const bought = prescribed.buyRepetition(1, day1)
    expect(bought.logMiss('MISSED', sunday(0)).lotPills(sunday(0))).toBe(0)
    const emptied = bought.refillWeekly(1, sunday(0)).refillWeekly(1, sunday(1)).deleteRefill(2).deleteRefill(1)
    expect(emptied.lotPills(sunday(1))).toBe(0)
    expect(emptied.refillWeekly(1, sunday(1)).lotPills(sunday(1))).toBe(7)
  })

  test("an older refill's pills are used up by now", () => {
    const data = prescribed.buyRepetition(1, day1).refillWeekly(1, sunday(0)).refillWeekly(1, sunday(1))
    expect(data.deleteRefill(1).lotPills(sunday(1))).toBe(7)
  })

  test("an empty box can't be refilled", () => {
    let data = new D().addPrescription(draft(7, 1, 1), day1).buyRepetition(1, day1).refillWeekly(1, evening(day1))
    expect(boxStatus(data.boxes[0])).toBe('FINISHED')
    data = data.refillWeekly(1, evening(day1))
    expect(data.refills.length).toBe(1)
  })

  test('box status follows pills left', () => {
    const bought = prescribed.buyRepetition(1, day1)
    expect(boxStatus(bought.boxes[0])).toBe('NOT_STARTED')
    const refilled = bought.refillWeekly(1, evening(day1))
    expect(boxStatus(refilled.boxes[0])).toBe('IN_PROGRESS')
    expect(refilled.boxes[0].pillsLeft).toBe(23)
  })

  test('saving as extra disables refills and undoing restores the status', () => {
    const started = prescribed.buyRepetition(1, day1).refillWeekly(1, evening(day1))
    const extra = started.saveAsExtra(1).saveAsExtra(2)
    expect(boxStatus(extra.boxes[0])).toBe('EXTRA')
    expect(extra.refillWeekly(1, evening(day1)).data).toBe(extra.data)
    const restored = extra.backToInventory(1).backToInventory(2)
    expect(boxStatus(restored.boxes[0])).toBe('IN_PROGRESS')
    expect(boxStatus(restored.boxes[1])).toBe('NOT_STARTED')
  })

  test("an empty box can't be saved as extra", () => {
    const data = new D().addPrescription(draft(7, 1, 1), day1).buyRepetition(1, day1).refillWeekly(1, evening(day1))
    expect(data.saveAsExtra(1).data).toBe(data.data)
  })

  test('misses are logged with their date and kind', () => {
    const data = new D().logMiss('MISSED', evening(day1)).logMiss('DELAYED', evening(day2))
    expect(data.misses).toEqual([
      { id: 1, date: day1, kind: 'MISSED', time: null },
      { id: 2, date: day2, kind: 'DELAYED', time: null },
    ])
  })

  const refilled = new D()
    .addPrescription(draft(), day1)
    .buyRepetition(1, day1)
    .refillWeekly(1, evening(day1))
    .refillWeekly(1, evening(day2))

  test('editing a prescription replaces its fields and works out its status', () => {
    const prescription = prescribed.prescriptions[0]
    const [edited] = prescribed.editPrescription({ ...prescription, prescribedBy: 'Dr Other', orderedDate: day2 })
      .prescriptions
    expect(edited.prescribedBy).toBe('Dr Other')
    expect(edited.status).toBe('ORDERED')
  })

  test('a prescription with every repetition bought is finished and no longer ordered', () => {
    const prescription = prescribed.prescriptions[0]
    const [edited] = prescribed.editPrescription({
      ...prescription, repetitionsBought: prescription.repetitionsTotal, orderedDate: day2,
    }).prescriptions
    expect(edited.status).toBe('FINISHED')
    expect(edited.orderedDate).toBeNull()
  })

  test('an edit with more repetitions bought than in total changes nothing', () => {
    const prescription = prescribed.prescriptions[0]
    const edited = prescribed.editPrescription({ ...prescription, repetitionsBought: prescription.repetitionsTotal + 1 })
    expect(edited.data).toBe(prescribed.data)
  })

  test('deleting a prescription deletes its boxes and refills only', () => {
    const data = refilled.addPrescription(draft(), day1).buyRepetition(2, day1)
      .refillWeekly(refilled.boxes.length + 1, evening(day2))
    const deleted = data.deletePrescription(1)
    expect(deleted.prescriptions.map(it => it.id)).toEqual([2])
    expect(deleted.boxes.every(it => it.prescriptionId === 2)).toBe(true)
    expect(deleted.refills.length > 0 && deleted.refills.every(it => it.prescriptionId === 2)).toBe(true)
  })

  test('deleting only a prescription keeps its boxes and refills, without links to it', () => {
    const deleted = refilled.deletePrescription(1, true)
    expect(deleted.prescriptions).toEqual([])
    expect(deleted.boxes.length).toBe(refilled.boxes.length)
    expect(deleted.boxes.every(it => it.prescriptionId === null)).toBe(true)
    expect(deleted.refills.map(it => it.boxId)).toEqual(refilled.refills.map(it => it.boxId))
    expect(deleted.refills.every(it => it.prescriptionId === null)).toBe(true)
  })

  test("a new prescription doesn't take over the boxes of a deleted one with the same id", () => {
    const data = refilled.deletePrescription(1, true).addPrescription(draft(), day2)
    expect(data.prescriptions[0].id).toBe(1)
    expect(data.boxes.some(it => it.prescriptionId === 1)).toBe(false)
  })

  test('deleting a prescription keeps refills whose box was kept when it was deleted', () => {
    const data = refilled.deleteBox(1, true).deletePrescription(1)
    expect(data.refills.length).toBe(refilled.refills.length)
    expect(data.refills.every(it => it.boxId === null && it.prescriptionId === null)).toBe(true)
  })

  test('buying gives each box its expiry date', () => {
    const dates = ['2030-01', '2030-01', '2031-06']
    expect(prescribed.buyRepetition(1, day1, dates).boxes.map(it => it.expiryDate)).toEqual(dates)
  })

  test("a box's first refill sets its open date, and later ones keep it", () => {
    const bought = prescribed.buyRepetition(1, day1)
    expect(bought.boxes[0].openDate).toBeNull()
    const opened = bought.refillWeekly(1, evening(day1)).refillWeekly(1, evening(day2))
    expect(opened.boxes[0].openDate).toBe(day1)
  })

  test('deleting the only refill makes the box not opened again', () => {
    const opened = prescribed.buyRepetition(1, day1).refillWeekly(1, evening(day1))
    expect(opened.deleteRefill(opened.refills[0].id).boxes[0].openDate).toBeNull()
  })

  test("a box already under way without an open date doesn't get one from a later refill", () => {
    const bought = prescribed.buyRepetition(1, day1)
    const underWay = bought.editBox({ ...bought.boxes[0], pillsLeft: 20 })
    expect(underWay.refillWeekly(1, evening(day2)).boxes[0].openDate).toBeNull()
  })

  test('editing a box replaces its fields, and more pills left than in total changes nothing', () => {
    const box = refilled.boxes[0]
    expect(refilled.editBox({ ...box, pillsLeft: 5 }).boxes[0].pillsLeft).toBe(5)
    expect(refilled.editBox({ ...box, pillsLeft: box.pillsTotal + 1 }).data).toBe(refilled.data)
  })

  test('an extra box edited down to empty is no longer extra', () => {
    const data = refilled.saveAsExtra(1)
    expect(boxStatus(data.editBox({ ...data.boxes[0], pillsLeft: 0 }).boxes[0])).toBe('FINISHED')
  })

  test('an extra box keeps its notes until it goes back to the inventory', () => {
    const noted = refilled.saveAsExtra(1).setBoxNotes(1, ' For travel ')
    expect(noted.boxes[0].notes).toBe('For travel')
    expect(noted.backToInventory(1).boxes[0].notes).toBe('')
  })

  test("a box that isn't extra can't have notes", () => {
    expect(refilled.setBoxNotes(1, 'For travel').data).toBe(refilled.data)
  })

  test('deleting a box deletes its refills', () => {
    const deleted = refilled.deleteBox(1)
    expect(deleted.boxes.some(it => it.id === 1)).toBe(false)
    expect(deleted.refills).toEqual([])
  })

  test('deleting only a box keeps its refills, without a link to it', () => {
    const deleted = refilled.deleteBox(1, true)
    expect(deleted.boxes.some(it => it.id === 1)).toBe(false)
    expect(deleted.refills.length).toBe(refilled.refills.length)
    expect(deleted.refills.every(it => it.boxId === null && it.prescriptionId === 1)).toBe(true)
  })

  test("refills without a box don't affect each other", () => {
    const data = refilled.refillWeekly(1, evening(day2)).deleteBox(1, true)
    const first = data.refills[0]
    expect(data.deleteRefill(first.id).refills).toEqual(data.refills.slice(1))
  })

  test("changing a refill's pills left moves pills, and later refills follow", () => {
    const pillsBefore = refilled.boxes[0].pillsLeft
    const first = refilled.refills[0]
    const edited = refilled.editRefill({ ...first, amountRemaining: first.amountRemaining - 2 })
    expect(edited.boxes[0].pillsLeft).toBe(pillsBefore - 2)
    expect(edited.refills[0].amountRemaining).toBe(first.amountRemaining - 2)
    expect(edited.refills.at(-1)!.amountRemaining).toBe(refilled.refills.at(-1)!.amountRemaining - 2)
  })

  test("changing only a refill's amount leaves its box alone", () => {
    const first = refilled.refills[0]
    const edited = refilled.editRefill({ ...first, amount: first.amount - 1 })
    expect(edited.refills[0].amount).toBe(first.amount - 1)
    expect(edited.boxes).toEqual(refilled.boxes)
  })

  test("a refill's pills left can't leave its box outside its limits", () => {
    const first = refilled.refills[0]
    const box = refilled.boxes[0]
    const tooFew = first.amountRemaining - box.pillsLeft - 1
    const tooMany = first.amountRemaining + box.pillsTotal - box.pillsLeft + 1
    expect(refilled.editRefill({ ...first, amountRemaining: tooFew }).data).toBe(refilled.data)
    expect(refilled.editRefill({ ...first, amountRemaining: tooMany }).data).toBe(refilled.data)
    expect(refilled.editRefill({ ...first, amountRemaining: -1 }).data).toBe(refilled.data)
  })

  test("a refill can't be edited to more pills than its box has", () => {
    const first = refilled.refills[0]
    const edited = refilled.editRefill({ ...first, amount: first.amount + refilled.boxes[0].pillsLeft + 1 })
    expect(edited.data).toBe(refilled.data)
  })

  test('deleting a refill puts its pills back, and later refills follow', () => {
    const pillsBefore = refilled.boxes[0].pillsLeft
    const first = refilled.refills[0]
    const deleted = refilled.deleteRefill(first.id)
    expect(deleted.boxes[0].pillsLeft).toBe(pillsBefore + first.amount)
    expect(deleted.refills).toHaveLength(1)
    expect(deleted.refills[0].amountRemaining).toBe(refilled.refills.at(-1)!.amountRemaining + first.amount)
  })

  test('refills follow date order once a date is edited', () => {
    const [first, second] = refilled.refills
    const moved = refilled.editRefill({ ...second, date: '2026-01-04' })
    const deleted = moved.deleteRefill(first.id)
    expect(deleted.refills[0].amountRemaining).toBe(second.amountRemaining)
  })

  test('a miss can be edited and deleted', () => {
    const data = new D().logMiss('MISSED', evening(day1))
    const miss = data.misses[0]
    const edited = data.editMiss({ ...miss, date: day2, kind: 'DELAYED' })
    expect(edited.misses).toEqual([{ ...miss, date: day2, kind: 'DELAYED' }])
    expect(edited.deleteMiss(miss.id).misses).toEqual([])
  })

  test('dates step across months and years', () => {
    expect(plusDays('2026-12-31', 1)).toBe('2027-01-01')
    expect(plusDays('2028-03-01', -1)).toBe('2028-02-29')
  })
})
