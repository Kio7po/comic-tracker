import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

// index.ts initializes i18n as a side effect of being imported, so each test sets up the browser
// state first and then imports a fresh copy of the module.
async function loadI18n(browserLanguages: string[], stored?: string) {
  vi.spyOn(navigator, 'languages', 'get').mockReturnValue(browserLanguages);
  if (stored !== undefined) {
    localStorage.setItem('language', stored);
  }
  vi.resetModules();
  return import('./index');
}

describe('i18n language selection', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    document.documentElement.lang = '';
  });

  it('starts in the browser language when nothing is stored', async () => {
    const { default: i18n } = await loadI18n(['es-ES']);

    expect(i18n.language).toBe('es');
    expect(document.documentElement.lang).toBe('es');
  });

  it('falls back to English when the browser language is not supported', async () => {
    const { default: i18n } = await loadI18n(['fr-FR']);

    expect(i18n.language).toBe('en');
  });

  it('uses the first supported language in the browser preference order', async () => {
    const { default: i18n } = await loadI18n(['fr-FR', 'es-ES', 'en-US']);

    expect(i18n.language).toBe('es');
  });

  it('prefers the stored choice over the browser language', async () => {
    const { default: i18n } = await loadI18n(['es-ES'], 'en');

    expect(i18n.language).toBe('en');
  });

  it('ignores a stored value that is not a supported language', async () => {
    const { default: i18n, getStoredLanguage } = await loadI18n(['es-ES'], 'xx');

    expect(getStoredLanguage()).toBeNull();
    expect(i18n.language).toBe('es');
  });

  it('behaves as no stored choice when storage throws', async () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('storage disabled');
    });

    const { default: i18n, getStoredLanguage } = await loadI18n(['es-ES']);

    expect(getStoredLanguage()).toBeNull();
    expect(i18n.language).toBe('es');
  });

  it('setLanguage stores the choice, switches the language and updates <html lang>', async () => {
    const { default: i18n, setLanguage } = await loadI18n(['es-ES']);

    await setLanguage('en');

    expect(localStorage.getItem('language')).toBe('en');
    expect(i18n.language).toBe('en');
    expect(document.documentElement.lang).toBe('en');
  });

  it("setLanguage('auto') clears the stored choice and goes back to the browser language", async () => {
    const { default: i18n, setLanguage } = await loadI18n(['es-ES'], 'en');

    await setLanguage('auto');

    expect(localStorage.getItem('language')).toBeNull();
    expect(i18n.language).toBe('es');
    expect(document.documentElement.lang).toBe('es');
  });

  it('setLanguage still switches the language when storage throws on write', async () => {
    const { default: i18n, setLanguage } = await loadI18n(['es-ES']);
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('quota exceeded');
    });

    await setLanguage('en');

    expect(i18n.language).toBe('en');
  });
});
