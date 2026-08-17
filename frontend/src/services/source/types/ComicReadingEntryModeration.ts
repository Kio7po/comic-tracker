import type { ComicSummary } from '@/services/comic/types';
import type { ContributorSummary } from '@/services/user/types';
import type { ComicReadingEntry } from './ComicReadingEntry';

export interface ComicReadingEntryModeration {
  entry: ComicReadingEntry;
  comic: ComicSummary;
  contributedBy: ContributorSummary;
}