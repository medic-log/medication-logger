/** The longest title, message and email the feedback Worker accepts. */
export const MAX_TITLE_LENGTH = 120
export const MAX_MESSAGE_LENGTH = 5000
export const MAX_EMAIL_LENGTH = 254

const TIMEOUT_MILLIS = 10_000

// The same check as the Worker's
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Whether [email] looks like an address the Worker accepts. */
export function isValidEmail(email: string): boolean {
  return email.length <= MAX_EMAIL_LENGTH && EMAIL.test(email)
}

/**
 * Feedback for the Cloudflare feedback Worker, which turns it into an issue; topic is one of its topics, and email,
 * if not blank, is where to answer.
 */
export interface Feedback {
  title: string
  topic: string
  version: string
  message: string
  email: string
}

/** How sending went: OFFLINE and UNAVAILABLE never reached the Worker, REJECTED did but it said no. */
export type SendResult = 'SENT' | 'OFFLINE' | 'UNAVAILABLE' | 'REJECTED'

/** The result of the Worker answering with [code]: busy (429) and server errors are worth retrying later. */
export function sendResultOf(code: number): SendResult {
  if (code >= 200 && code <= 299) return 'SENT'
  if (code === 429 || (code >= 500 && code <= 599)) return 'UNAVAILABLE'
  return 'REJECTED'
}

/** The JSON the Worker reads, marked as sent from the web, like the site's feedback form. */
export function feedbackJson(feedback: Feedback): string {
  const { email, ...rest } = feedback
  return JSON.stringify({ ...rest, source: 'web', ...(email.trim() !== '' ? { email } : {}) })
}

/** Posts [feedback] to [url] and tells how it went. */
export async function sendFeedback(feedback: Feedback, url: string): Promise<SendResult> {
  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: feedbackJson(feedback),
      signal: AbortSignal.timeout(TIMEOUT_MILLIS),
    })
    return sendResultOf(response.status)
  } catch {
    // fetch fails without an answer both offline and when the server can't be reached
    return navigator.onLine ? 'UNAVAILABLE' : 'OFFLINE'
  }
}
