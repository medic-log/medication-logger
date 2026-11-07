import { DataFormatError, dataToJson, jsonToData } from './json'
import { type MedicationData, emptyData } from './MedicationData'

/** Keeps the app's data between runs; tests replace it with an in-memory one. */
export interface MedicationStore {
  load(): MedicationData
  /** Throws when the data can't be written, such as when storage is full. */
  save(data: MedicationData): void
}

/** The key the data is saved under, as the Android app's medication-data.json. */
export const DATA_KEY = 'medication-data'

/**
 * Stores everything as one JSON text in [storage], in the Android app's file format. Unreadable data is set aside
 * under its own key, as starting empty would overwrite it on the next save, so it's kept for recovery.
 */
export class LocalStorageMedicationStore implements MedicationStore {
  constructor(private readonly storage: Storage) {}

  load(): MedicationData {
    const text = this.storage.getItem(DATA_KEY)
    if (text === null) return emptyData()
    try {
      return jsonToData(JSON.parse(text))
    } catch (error) {
      if (!(error instanceof SyntaxError || error instanceof DataFormatError)) throw error
      this.storage.setItem(`${DATA_KEY}.unreadable`, text)
      this.storage.removeItem(DATA_KEY)
      return emptyData()
    }
  }

  save(data: MedicationData): void {
    this.storage.setItem(DATA_KEY, JSON.stringify(dataToJson(data)))
  }
}
