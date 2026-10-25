import { describe, expect, it } from 'vitest';
import { createLanguageNameFormatter, languageName } from './languageName';

describe('languageName', () => {
  it('capitalizes names that Intl returns in lowercase', () => {
    expect(languageName('es', 'es')).toBe('Español');
    expect(languageName('en', 'es')).toBe('Inglés');
  });

  it('keeps names that already start in uppercase', () => {
    expect(languageName('en', 'en')).toBe('English');
    expect(languageName('es', 'en')).toBe('Spanish');
  });

  it('returns the code untouched when it is not a valid language code', () => {
    expect(languageName('not a code!', 'en')).toBe('not a code!');
  });

  it('formats several codes with one formatter', () => {
    const format = createLanguageNameFormatter('es');

    expect(['es', 'en', 'fr'].map(format)).toEqual(['Español', 'Inglés', 'Francés']);
  });
});
