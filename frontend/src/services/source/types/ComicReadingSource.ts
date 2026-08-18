import type { ComicReadingSourceStatus } from './ComicReadingSourceStatus';

export interface ComicReadingSource {
  id: number;
  slug: string;
  name: string;
  url: string;
  iconUrl: string | null;
  status: ComicReadingSourceStatus;
  createdAt: string;
}