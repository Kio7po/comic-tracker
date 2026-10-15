import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import en from './locales/en/translation.json';
import es from './locales/es/translation.json';

export const SUPPORTED_LANGUAGES = ['en', 'es'] as const;
export type AppLanguage = (typeof SUPPORTED_LANGUAGES)[number];

const FALLBACK_LANGUAGE: AppLanguage = 'en';
const STORAGE_KEY = 'language';

function isSupported(value: string | null | undefined): value is AppLanguage {
  return SUPPORTED_LANGUAGES.includes(value as AppLanguage);
}

// The explicit choice saved by setLanguage, or null when the user follows the browser.
export function getStoredLanguage(): AppLanguage | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return isSupported(stored) ? stored : null;
  } catch {
    return null; // Storage disabled, behaves as "no explicit choice".
  }
}

// The first of the browser's preferred languages we support ('es-ES' -> 'es'); otherwise English.
function detectBrowserLanguage(): AppLanguage {
  for (const tag of navigator.languages ?? [navigator.language]) {
    const base = tag.split('-')[0].toLowerCase();
    if (isSupported(base)) {
      return base;
    }
  }
  return FALLBACK_LANGUAGE;
}

void i18n.use(initReactI18next).init({
  lng: getStoredLanguage() ?? detectBrowserLanguage(),
  fallbackLng: FALLBACK_LANGUAGE,
  supportedLngs: SUPPORTED_LANGUAGES,
  resources: {
    en: { translation: en },
    es: { translation: es },
  },
  interpolation: {
    escapeValue: false, // React already escapes output
  },
});

// Keeps <html lang> in sync (index.html ships a static "en"), for screen readers and the
// browser's own spellcheck/translation features.
document.documentElement.lang = i18n.language;
i18n.on('languageChanged', (language) => {
  document.documentElement.lang = language;
});

// 'auto' means the browser language should be used
export type LanguagePreference = AppLanguage | 'auto';

// Only an explicit choice is persisted: the auto-detected language is deliberately not stored,
// so a user who never chooses keeps following their browser. 'auto' clears a previous choice
// and goes back to that.
export function setLanguage(preference: LanguagePreference) {
  try {
    if (preference === 'auto') {
      localStorage.removeItem(STORAGE_KEY);
    } else {
      localStorage.setItem(STORAGE_KEY, preference);
    }
  } catch {
    // Storage disabled or full, the choice just won't survive a reload.
  }
  return i18n.changeLanguage(preference === 'auto' ? detectBrowserLanguage() : preference);
}

export default i18n;
