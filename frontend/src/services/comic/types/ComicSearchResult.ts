import type { ComicMediaType } from './ComicMediaType';
import type { ComicStatus } from './ComicStatus';
import type { NsfwRating } from './NsfwRating';

export interface ComicSearchResult {
  sourceSlug: string;
  externalId: string;
  title: string;
  synopsis: string | null;
  coverUrl: string | null;
  mediaType: ComicMediaType | null;
  status: ComicStatus | null;
  nsfw: NsfwRating | null;
}
