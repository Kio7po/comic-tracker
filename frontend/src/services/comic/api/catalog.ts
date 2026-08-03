import { apiFetch } from '@/common/api/client';
import type { PageResponse } from '@/common/api/PageResponse';
import type { Comic, ComicMediaType, ComicSearchResult, ComicStatus, NsfwRating } from '../types';

export interface SearchParams {
  keywords: string;
  limit?: number;
  offset?: number;
  nsfw?: NsfwRating;
  status?: ComicStatus;
  type?: ComicMediaType;
}

export function search(params: SearchParams): Promise<PageResponse<ComicSearchResult>> {
  const query = new URLSearchParams({ keywords: params.keywords });
  if (params.limit !== undefined) query.set('limit', String(params.limit));
  if (params.offset !== undefined) query.set('offset', String(params.offset));
  if (params.nsfw) query.set('nsfw', params.nsfw);
  if (params.status) query.set('status', params.status);
  if (params.type) query.set('type', params.type);

  return apiFetch<PageResponse<ComicSearchResult>>(`/catalog/search?${query.toString()}`);
}

export function importComic(sourceSlug: string, externalId: string): Promise<Comic> {
  return apiFetch<Comic>(
    `/catalog/${encodeURIComponent(sourceSlug)}/${encodeURIComponent(externalId)}`,
    { method: 'POST' },
  );
}
