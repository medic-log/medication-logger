// The site's translations, in the app's languages. The language comes from ?lang=, then the one chosen
// in the page's language menu, then the browser's, else English.
// Elements name their string with data-i18n (text), data-i18n-html (markup), data-i18n-placeholder,
// data-i18n-aria-label or data-i18n-content (meta tags); pages read others with i18n.t(key).
const STRINGS = {
  en: {
    language: 'Language',
    'index.description': 'Track your prescriptions, pill boxes and refills, with reminders. Android app.',
    'index.intro': 'Track your prescriptions, pill boxes and weekly refills, with reminders for refills, pickups and running out. Your data stays on your device.',
    'index.download': 'Download APK (Android)',
    'index.latest': 'Latest version:',
    'index.unavailable': 'unavailable',
    'index.requires': 'Requires Android 9 or newer.',
    'index.install': 'How to install',
    'index.step1': 'Open the downloaded <code>medication-logger.apk</code>.',
    'index.step2': 'If Android asks, allow your browser or file manager to install unknown apps.',
    'index.step3': 'Tap Install. To update later, download and install the new version the same way; your data is kept.',
    'feedback.description': 'Send feedback about the Medication logger Android app.',
    'feedback.intro': 'Tell us about a bug, a translation error, an idea or anything else about <a href="../">Medication logger</a>.',
    'feedback.title': 'Title',
    'feedback.topic': 'Topic',
    'feedback.topic.translation': 'Translation',
    'feedback.topic.bug': 'Bug',
    'feedback.topic.suggestion': 'Suggestion',
    'feedback.topic.help': 'Help',
    'feedback.topic.question': 'Question',
    'feedback.topic.other': 'Other',
    'feedback.email': 'Email (optional, to answer you)',
    'feedback.version': 'Version',
    'feedback.versionHint': 'Settings shows it at the bottom',
    'feedback.message': 'Message',
    'feedback.send': 'Send',
    'feedback.contents': 'Sends the title, topic, version, message and your email if you give one, nothing from your medication records.',
    'feedback.sending': 'Sending…',
    'feedback.sent': 'Feedback sent. Thank you!',
    'feedback.offline': "Couldn't send feedback. Check your internet connection and try again.",
    'feedback.unavailable': "The feedback server isn't available right now. Try again later.",
    'feedback.rejected': "The feedback server didn't accept this feedback. Reload the page and try again.",
  },
  es: {
    language: 'Idioma',
    'index.description': 'Registra tus recetas, pastilleros y reposiciones, con recordatorios. App para Android.',
    'index.intro': 'Registra tus recetas, pastilleros y reposiciones semanales, con recordatorios para reponer, recoger y antes de quedarte sin pastillas. Tus datos se quedan en tu dispositivo.',
    'index.download': 'Descargar APK (Android)',
    'index.latest': 'Última versión:',
    'index.unavailable': 'no disponible',
    'index.requires': 'Requiere Android 9 o posterior.',
    'index.install': 'Cómo instalar',
    'index.step1': 'Abre el archivo descargado <code>medication-logger.apk</code>.',
    'index.step2': 'Si Android lo pide, permite que tu navegador o gestor de archivos instale aplicaciones desconocidas.',
    'index.step3': 'Toca Instalar. Para actualizar más adelante, descarga e instala la nueva versión de la misma forma; tus datos se conservan.',
    'feedback.description': 'Envía feedback sobre la app para Android Medication logger.',
    'feedback.intro': 'Cuéntanos sobre un error, un fallo de traducción, una idea o cualquier otra cosa sobre <a href="../">Medication logger</a>.',
    'feedback.title': 'Título',
    'feedback.topic': 'Tema',
    'feedback.topic.translation': 'Traducción',
    'feedback.topic.bug': 'Error',
    'feedback.topic.suggestion': 'Sugerencia',
    'feedback.topic.help': 'Ayuda',
    'feedback.topic.question': 'Pregunta',
    'feedback.topic.other': 'Otro',
    'feedback.email': 'Correo (opcional, para responderte)',
    'feedback.version': 'Versión',
    'feedback.versionHint': 'Aparece al final de Ajustes',
    'feedback.message': 'Mensaje',
    'feedback.send': 'Enviar',
    'feedback.contents': 'Se envían el título, el tema, la versión, el mensaje y tu correo si lo das, nada de tus registros de medicación.',
    'feedback.sending': 'Enviando…',
    'feedback.sent': 'Feedback enviado. ¡Gracias!',
    'feedback.offline': 'No se pudo enviar el feedback. Revisa tu conexión a internet y vuelve a intentarlo.',
    'feedback.unavailable': 'El servidor de feedback no está disponible ahora. Inténtalo más tarde.',
    'feedback.rejected': 'El servidor de feedback no aceptó este feedback. Recarga la página y vuelve a intentarlo.',
  },
  pt: {
    language: 'Idioma',
    'index.description': 'Acompanhe suas receitas, porta-comprimidos e reposições, com lembretes. App para Android.',
    'index.intro': 'Acompanhe suas receitas, porta-comprimidos e reposições semanais, com lembretes para repor, retirar e antes de acabar. Seus dados ficam no seu dispositivo.',
    'index.download': 'Baixar APK (Android)',
    'index.latest': 'Última versão:',
    'index.unavailable': 'indisponível',
    'index.requires': 'Requer Android 9 ou mais recente.',
    'index.install': 'Como instalar',
    'index.step1': 'Abra o arquivo baixado <code>medication-logger.apk</code>.',
    'index.step2': 'Se o Android pedir, permita que seu navegador ou gerenciador de arquivos instale apps desconhecidos.',
    'index.step3': 'Toque em Instalar. Para atualizar depois, baixe e instale a nova versão da mesma forma; seus dados são mantidos.',
    'feedback.description': 'Envie feedback sobre o app para Android Medication logger.',
    'feedback.intro': 'Conte-nos sobre um erro, um problema de tradução, uma ideia ou qualquer outra coisa sobre o <a href="../">Medication logger</a>.',
    'feedback.title': 'Título',
    'feedback.topic': 'Assunto',
    'feedback.topic.translation': 'Tradução',
    'feedback.topic.bug': 'Erro',
    'feedback.topic.suggestion': 'Sugestão',
    'feedback.topic.help': 'Ajuda',
    'feedback.topic.question': 'Pergunta',
    'feedback.topic.other': 'Outro',
    'feedback.email': 'E-mail (opcional, para responder a você)',
    'feedback.version': 'Versão',
    'feedback.versionHint': 'Aparece no final das Configurações',
    'feedback.message': 'Mensagem',
    'feedback.send': 'Enviar',
    'feedback.contents': 'São enviados o título, o assunto, a versão, a mensagem e seu e-mail se você informar, nada dos seus registros de medicação.',
    'feedback.sending': 'Enviando…',
    'feedback.sent': 'Feedback enviado. Obrigado!',
    'feedback.offline': 'Não foi possível enviar o feedback. Verifique sua conexão com a internet e tente novamente.',
    'feedback.unavailable': 'O servidor de feedback não está disponível agora. Tente novamente mais tarde.',
    'feedback.rejected': 'O servidor de feedback não aceitou este feedback. Recarregue a página e tente novamente.',
  },
  nl: {
    language: 'Taal',
    'index.description': 'Houd je recepten, pillendozen en aanvullingen bij, met herinneringen. Android-app.',
    'index.intro': 'Houd je recepten, pillendozen en wekelijkse aanvullingen bij, met herinneringen om aan te vullen, op te halen en voordat je op raakt. Je gegevens blijven op je apparaat.',
    'index.download': 'APK downloaden (Android)',
    'index.latest': 'Nieuwste versie:',
    'index.unavailable': 'niet beschikbaar',
    'index.requires': 'Vereist Android 9 of nieuwer.',
    'index.install': 'Zo installeer je het',
    'index.step1': 'Open het gedownloade <code>medication-logger.apk</code>.',
    'index.step2': 'Als Android erom vraagt, sta je browser of bestandsbeheerder toe onbekende apps te installeren.',
    'index.step3': 'Tik op Installeren. Om later bij te werken, download en installeer je de nieuwe versie op dezelfde manier; je gegevens blijven bewaard.',
    'feedback.description': 'Stuur feedback over de Android-app Medication logger.',
    'feedback.intro': 'Vertel ons over een fout, een vertaalfout, een idee of iets anders over <a href="../">Medication logger</a>.',
    'feedback.title': 'Titel',
    'feedback.topic': 'Onderwerp',
    'feedback.topic.translation': 'Vertaling',
    'feedback.topic.bug': 'Fout',
    'feedback.topic.suggestion': 'Suggestie',
    'feedback.topic.help': 'Hulp',
    'feedback.topic.question': 'Vraag',
    'feedback.topic.other': 'Anders',
    'feedback.email': 'E-mail (optioneel, om je te antwoorden)',
    'feedback.version': 'Versie',
    'feedback.versionHint': 'Staat onderaan in Instellingen',
    'feedback.message': 'Bericht',
    'feedback.send': 'Versturen',
    'feedback.contents': 'Verstuurt de titel, het onderwerp, de versie, het bericht en je e-mail als je die invult, niets uit je medicatiegegevens.',
    'feedback.sending': 'Versturen…',
    'feedback.sent': 'Feedback verstuurd. Bedankt!',
    'feedback.offline': 'Feedback versturen is mislukt. Controleer je internetverbinding en probeer het opnieuw.',
    'feedback.unavailable': 'De feedbackserver is nu niet beschikbaar. Probeer het later opnieuw.',
    'feedback.rejected': 'De feedbackserver heeft deze feedback niet geaccepteerd. Laad de pagina opnieuw en probeer het nog eens.',
  },
};

const i18n = (() => {
  const KEY = 'language';
  const supported = code => (code && STRINGS[code.slice(0, 2).toLowerCase()] ? code.slice(0, 2).toLowerCase() : null);
  const stored = () => { try { return localStorage.getItem(KEY); } catch { return null; } };
  let language = supported(new URLSearchParams(location.search).get('lang'))
    || supported(stored())
    || navigator.languages.map(supported).find(Boolean)
    || 'en';

  const t = key => STRINGS[language][key] ?? STRINGS.en[key];
  const apply = () => {
    document.documentElement.lang = language;
    document.querySelectorAll('[data-i18n]').forEach(el => { el.textContent = t(el.dataset.i18n); });
    document.querySelectorAll('[data-i18n-html]').forEach(el => { el.innerHTML = t(el.dataset.i18nHtml); });
    document.querySelectorAll('[data-i18n-placeholder]').forEach(el => { el.placeholder = t(el.dataset.i18nPlaceholder); });
    document.querySelectorAll('[data-i18n-aria-label]').forEach(el => { el.ariaLabel = t(el.dataset.i18nAriaLabel); });
    document.querySelectorAll('[data-i18n-content]').forEach(el => { el.content = t(el.dataset.i18nContent); });
  };

  // The page's language menu: changing it saves the choice for both pages
  const menu = document.getElementById('language');
  if (menu) {
    menu.value = language;
    menu.addEventListener('change', () => {
      language = menu.value;
      try { localStorage.setItem(KEY, language); } catch {}
      apply();
    });
  }
  apply();
  return { t };
})();
