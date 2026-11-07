import { type AppLanguage, APP_LANGUAGES } from '../../i18n/i18n'

/** The app's languages as radio buttons, each named in itself, to be found from any language. */
export function LanguageOptions({ selected, onSelect }: { selected: AppLanguage; onSelect: (language: AppLanguage) => void }) {
  return (
    <div role="radiogroup">
      {APP_LANGUAGES.map(language => (
        <button
          key={language.tag}
          type="button"
          role="radio"
          aria-checked={language.tag === selected}
          className="radio-row"
          lang={language.tag}
          onClick={() => onSelect(language.tag)}
        >
          <span className="radio" />
          <span className="label body-large">{language.nativeName}</span>
        </button>
      ))}
    </div>
  )
}
