import { DataFormatError, jsonToRecords, recordsToJson } from './json'
import type { MedicationData } from './MedicationData'

const INDENT = 2

/** Writes the records, without reminder settings or tutorial progress, to move them between devices and apps. */
export function writeExport(data: MedicationData): string {
  return JSON.stringify(recordsToJson(data), null, INDENT)
}

/** The records in [text], with everything else at its default, or null when it isn't an export. */
export function readExport(text: string): MedicationData | null {
  try {
    return jsonToRecords(JSON.parse(text))
  } catch (error) {
    if (error instanceof SyntaxError || error instanceof DataFormatError) return null
    throw error
  }
}
