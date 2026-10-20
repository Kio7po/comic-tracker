// Intl.DisplayNames construction isn't free (locale data resolution) — reuse one instance across
// a batch of lookups (e.g. mapping a whole list of codes) via createLanguageNameFormatter, rather
// than constructing one per code. languageName() is the convenience wrapper for a single lookup.
//
// The first letter is capitalized: Intl follows each language's own convention for writing
// language names, so some locales return them lowercase ("español"), which looks inconsistent
// next to others ("English") whenever these show up as labels or list options.
export function createLanguageNameFormatter(locale: string): (code: string) => string {
  const displayNames = new Intl.DisplayNames([locale], { type: 'language' });
  return (code: string) => {
    try {
      const name = displayNames.of(code);
      return name ? name.charAt(0).toLocaleUpperCase(locale) + name.slice(1) : code;
    } catch {
      return code;
    }
  };
}

export function languageName(code: string, locale: string): string {
  return createLanguageNameFormatter(locale)(code);
}
