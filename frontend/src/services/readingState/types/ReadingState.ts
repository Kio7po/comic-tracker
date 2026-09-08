import type { ComicReadingEntrySummary } from '@/services/source/types';
import type { ReadingStateStatus } from './ReadingStateStatus';

export interface ReadingState {
  id: number;
  status: ReadingStateStatus;
  chapters: number;
  notes: string | null;
  preferredEntry: ComicReadingEntrySummary | null;
  createdAt: string;
  updatedAt: string;
}
