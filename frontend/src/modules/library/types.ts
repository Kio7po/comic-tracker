import type { LibraryComic } from '@/services/comic/types';
import type { ReadingState } from '@/services/readingState/types';

// Mirrors the real ReadingStateWithComic shape (readingState + LibraryComic).
export interface LibraryEntry {
  readingState: ReadingState;
  comic: LibraryComic;
}
