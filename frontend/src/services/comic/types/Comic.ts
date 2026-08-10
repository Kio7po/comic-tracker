import type { ComicMediaType } from './ComicMediaType';
import type { ComicStatus } from './ComicStatus';
import type { NsfwRating } from './NsfwRating';

export interface Comic {
  id: number;
  slug: string;
  title: string;
  synopsis: string | null;
  coverUrl: string | null;
  alternativeTitles: string[];
  startDate: string | null;
  endDate: string | null;
  nsfw: NsfwRating | null;
  mediaType: ComicMediaType | null;
  status: ComicStatus | null;
  chapters: number | null;
  authors: string[];
  genres: string[];
  tags: string[];
}
