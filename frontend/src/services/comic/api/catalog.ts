import { apiFetch } from '@/common/api/client';
import type { PageResponse } from '@/common/api/PageResponse';
import type { SortDirection } from '@/common/api/SortDirection';
import type { Comic, ComicMediaType, ComicSearchResult, ComicSearchSortField, ComicStatus, NsfwRating } from '../types';

export interface SearchParams {
  keywords: string;
  limit?: number;
  offset?: number;
  nsfw?: NsfwRating;
  status?: ComicStatus;
  type?: ComicMediaType;
  sortBy?: ComicSearchSortField;
  direction?: SortDirection;
}

export function search(
  params: SearchParams,
  options?: { signal?: AbortSignal },
): Promise<PageResponse<ComicSearchResult>> {
  const query = new URLSearchParams({ keywords: params.keywords });
  if (params.limit !== undefined) query.set('limit', String(params.limit));
  if (params.offset !== undefined) query.set('offset', String(params.offset));
  if (params.nsfw) query.set('nsfw', params.nsfw);
  if (params.status) query.set('status', params.status);
  if (params.type) query.set('type', params.type);
  if (params.sortBy) query.set('sortBy', params.sortBy);
  if (params.direction) query.set('direction', params.direction);

  return apiFetch<PageResponse<ComicSearchResult>>(`/catalog/search?${query.toString()}`, {
    signal: options?.signal,
  });
}

export function importComic(sourceSlug: string, externalId: string): Promise<Comic> {
  return apiFetch<Comic>(
    `/catalog/${encodeURIComponent(sourceSlug)}/${encodeURIComponent(externalId)}`,
    { method: 'POST' },
  );
}
