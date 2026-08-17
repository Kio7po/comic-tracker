import type { ContributorSummary } from '@/services/user/types';
import type { ComicReadingSource } from './ComicReadingSource';

export interface ComicReadingSourceModeration {
  source: ComicReadingSource;
  contributedBy: ContributorSummary;
}