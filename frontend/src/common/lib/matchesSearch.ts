function normalize(value: string): string {
  return value
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();
}

// Case and accent insensitive, multi-word (every word must appear somewhere in the text, in any
// order). `text` can be a list of alternatives (e.g. a title plus its translations), each word
// only needs to appear in one of them, not all in the same one.
export function matchesSearch(text: string | string[], query: string): boolean {
  const normalizedQuery = normalize(query).trim();
  if (normalizedQuery === '') {
    return true;
  }

  const normalizedText = normalize(Array.isArray(text) ? text.join(' ') : text);
  // Split at whitespaces and check if every keyword is included
  return normalizedQuery.split(/\s+/).every((word) => normalizedText.includes(word));
}
