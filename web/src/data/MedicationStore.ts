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
 * under its own key with the time, as starting empty would overwrite it on the next save, so it's kept for recovery.
 */
export class LocalStorageMedicationStore implements MedicationStore {
  constructor(
    private readonly storage: Storage,
    private readonly now: () => Date = () => new Date(),
  ) {}

  load(): MedicationData {
    const text = this.storage.getItem(DATA_KEY)
    if (text === null) return emptyData()
    try {
      return jsonToData(JSON.parse(text))
    } catch (error) {
      if (!(error instanceof SyntaxError || error instanceof DataFormatError)) throw error
      // As 20261108T130525Z, in UTC like the Android app's file name
      const time = this.now().toISOString().replace(/[-:]|\.\d+/g, '')
      this.storage.setItem(`${DATA_KEY}.unreadable-${time}`, text)
      this.storage.removeItem(DATA_KEY)
      return emptyData()
    }
  }

  save(data: MedicationData): void {
    this.storage.setItem(DATA_KEY, JSON.stringify(dataToJson(data)))
  }
}
