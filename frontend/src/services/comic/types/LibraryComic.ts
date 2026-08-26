import type { ComicMediaType } from './ComicMediaType';
import type { ComicStatus } from './ComicStatus';
import type { NsfwRating } from './NsfwRating';

export interface LibraryComic {
  id: number;
  slug: string;
  title: string;
  alternativeTitles: string[];
  coverUrl: string | null;
  startDate: string | null;
  nsfw: NsfwRating | null;
  mediaType: ComicMediaType | null;
  status: ComicStatus | null;
  chapters: number | null;
}
