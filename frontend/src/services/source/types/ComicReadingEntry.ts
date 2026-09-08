import type { ComicReadingEntryStatus } from './ComicReadingEntryStatus';
import type { ComicReadingSource } from './ComicReadingSource';

export interface ComicReadingEntry {
  id: number;
  url: string;
  title: string | null;
  availableChapters: number | null;
  latestChapterAt: string | null;
  locale: string;
  status: ComicReadingEntryStatus;
  source: ComicReadingSource;
  createdAt: string;
}