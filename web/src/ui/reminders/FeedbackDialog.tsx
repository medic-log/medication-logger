import { useId, useState } from 'react'
import resources from 'virtual:android-resources'
import { MAX_EMAIL_LENGTH, MAX_MESSAGE_LENGTH, MAX_TITLE_LENGTH, type SendResult, isValidEmail, sendFeedback } from '../../data/Feedback'
import { useStrings } from '../../i18n/i18n'
import { IconButton, TextButton } from '../components/Buttons'
import { Dialog } from '../components/Dialog'
import { Icon } from '../components/Icon'
import { TextField } from '../components/TextField'
import { useToast } from '../components/Toast'

/**
 * What a piece of feedback is about; the same list as the feedback page on the download site. Help and
 * questions ask for an email to answer to.
 */
const TOPICS = [
  { topic: 'translation', label: 'feedback_topic_translation', isAnswerExpected: false },
  { topic: 'bug', label: 'feedback_topic_bug', isAnswerExpected: false },
  { topic: 'suggestion', label: 'feedback_topic_suggestion', isAnswerExpected: false },
  { topic: 'help', label: 'feedback_topic_help', isAnswerExpected: true },
  { topic: 'question', label: 'feedback_topic_question', isAnswerExpected: true },
  { topic: 'other', label: 'feedback_topic_other', isAnswerExpected: false },
]

const RESULT_TEXTS: Record<SendResult, string> = {
  SENT: 'feedback_sent',
  OFFLINE: 'feedback_send_offline',
  UNAVAILABLE: 'feedback_send_unavailable',
  REJECTED: 'feedback_send_rejected',
}

/**
 * A feedback form with the app's version filled in, not editable. Send posts it to the feedback Worker, which opens
 * an issue, and closes the dialog once it's sent. The button by the title offers to open the same form on the
 * download site, with the version passed along.
 */
export function FeedbackDialog({ onDismiss }: { onDismiss: () => void }) {
  const strings = useStrings()
  const showToast = useToast()
  const topicId = useId()
  const [title, setTitle] = useState('')
  const [topic, setTopic] = useState(TOPICS[2])
  const [message, setMessage] = useState('')
  const [email, setEmail] = useState('')
  const [isSending, setSending] = useState(false)
  const [isOpenSiteConfirmShown, setOpenSiteConfirmShown] = useState(false)
  // Optional, so only a filled-in one can be wrong
  const isEmailInvalid = topic.isAnswerExpected && email.trim() !== '' && !isValidEmail(email.trim())

  const send = async () => {
    setSending(true)
    const result = await sendFeedback(
      {
        title: title.trim(),
        topic: topic.topic,
        version: resources.versionName,
        message: message.trim(),
        email: topic.isAnswerExpected ? email.trim() : '',
      },
      resources.feedbackUrl,
    )
    setSending(false)
    showToast(strings.t(RESULT_TEXTS[result]), true)
    if (result === 'SENT') onDismiss()
  }

  return (
    <>
      <Dialog
        isForm
        title={
          <div className="dialog-title-row">
            <h2 className="dialog-title">{strings.t('settings_feedback')}</h2>
            <IconButton onClick={() => setOpenSiteConfirmShown(true)} aria-label={strings.t('feedback_open_site')}>
              <Icon name="ic_open_in_new" />
            </IconButton>
          </div>
        }
        onDismiss={onDismiss}
        buttons={
          <>
            <TextButton onClick={onDismiss}>{strings.t('edit_cancel')}</TextButton>
            <TextButton
              onClick={() => void send()}
              disabled={!title.trim() || !message.trim() || isEmailInvalid || isSending}
            >
              {strings.t('feedback_send')}
            </TextButton>
          </>
        }
      >
        <span>{strings.t('feedback_description')}</span>
        <TextField value={title} onChange={it => setTitle(it.slice(0, MAX_TITLE_LENGTH))} label={strings.t('feedback_title')} />
        <div className="field filled">
          <select
            id={topicId}
            value={topic.topic}
            onChange={event => setTopic(TOPICS.find(it => it.topic === event.target.value) ?? TOPICS[2])}
          >
            {TOPICS.map(option => (
              <option key={option.topic} value={option.topic}>
                {strings.t(option.label)}
              </option>
            ))}
          </select>
          <label htmlFor={topicId}>{strings.t('feedback_topic')}</label>
          <span className="trailing">
            <Icon name="arrow_drop_down" />
          </span>
        </div>
        {topic.isAnswerExpected && (
          <TextField
            value={email}
            onChange={it => setEmail(it.slice(0, MAX_EMAIL_LENGTH))}
            label={strings.t('feedback_email')}
            type="email"
            isError={isEmailInvalid}
            supportingText={isEmailInvalid ? strings.t('feedback_email_invalid') : undefined}
          />
        )}
        <TextField value={resources.versionName} onChange={() => {}} label={strings.t('feedback_version')} isDisabled />
        <TextField
          value={message}
          onChange={it => setMessage(it.slice(0, MAX_MESSAGE_LENGTH))}
          label={strings.t('feedback_message')}
          minLines={4}
        />
        <span className="body-small on-surface-variant">{strings.t('feedback_sent_contents')}</span>
      </Dialog>
      {isOpenSiteConfirmShown && (
        <Dialog
          title={strings.t('feedback_open_site_title')}
          onDismiss={() => setOpenSiteConfirmShown(false)}
          buttons={
            <>
              <TextButton onClick={() => setOpenSiteConfirmShown(false)}>{strings.t('edit_cancel')}</TextButton>
              <TextButton
                onClick={() => {
                  setOpenSiteConfirmShown(false)
                  const url = `${resources.siteUrl}feedback/?version=${encodeURIComponent(resources.versionName)}&lang=${strings.language}`
                  window.open(url, '_blank', 'noopener')
                }}
              >
                {strings.t('feedback_open')}
              </TextButton>
            </>
          }
        />
      )}
    </>
  )
}
