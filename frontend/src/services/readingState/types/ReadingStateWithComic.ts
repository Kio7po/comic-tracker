import type { ComicSummary } from '@/services/comic/types';
import type { ReadingState } from './ReadingState';

export interface ReadingStateWithComic {
  readingState: ReadingState;
  comic: ComicSummary;
}
