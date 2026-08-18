// Intl.DisplayNames construction isn't free (locale data resolution) — reuse one instance across
// a batch of lookups (e.g. mapping a whole list of codes) via createLanguageNameFormatter, rather
// than constructing one per code. languageName() is the convenience wrapper for a single lookup.
export function createLanguageNameFormatter(locale: string): (code: string) => string {
  const displayNames = new Intl.DisplayNames([locale], { type: 'language' });
  return (code: string) => {
    try {
      return displayNames.of(code) ?? code;
    } catch {
      return code;
    }
  };
}

export function languageName(code: string, locale: string): string {
  return createLanguageNameFormatter(locale)(code);
}