import type { ComicSummary } from '@/services/comic/types';
import type { ComicReadingEntry } from './ComicReadingEntry';

export interface ComicReadingEntryModeration {
  entry: ComicReadingEntry;
  comic: ComicSummary;
}