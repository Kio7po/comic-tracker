import type { ComicStatus } from '@/services/comic/types';

export interface ComicReadingSearchResultDetails {
  title: string | null;
  url: string | null;
  availableChapters: number | null;
  latestChapterAt: string | null;
  author: string | null;
  status: ComicStatus | null;
  synopsis: string | null;
}
