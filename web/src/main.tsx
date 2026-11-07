import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { MedicationRepository } from './data/MedicationRepository'
import { LocalStorageMedicationStore } from './data/MedicationStore'
import { localDateTimeOf } from './data/time'
import { App } from './ui/App'
import './ui/theme/theme.css'
import { listenForInstallPrompt } from './update/install'
import { Updates } from './update/updates'

listenForInstallPrompt()
// Asks the browser not to clear the records when it runs short of space; it may still decide on its own
void navigator.storage?.persist?.().catch(() => undefined)

const repository = new MedicationRepository(new LocalStorageMedicationStore(localStorage), () =>
  localDateTimeOf(new Date()),
)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App repository={repository} updates={new Updates()} />
  </StrictMode>,
)
