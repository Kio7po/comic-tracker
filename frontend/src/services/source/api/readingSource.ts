import { apiFetch } from '@/common/api/client';
import type { SortDirection } from '@/common/api/SortDirection';
import type {
  ComicReadingSearchListing,
  ComicReadingSearchResultDetails,
  ComicReadingSource,
  ComicReadingSourceModeration,
  ComicReadingSourceSortField,
  ComicReadingSourceStatus,
} from '../types';

export interface FindByStatusInParams {
  statuses: ComicReadingSourceStatus[];
  sortBy?: ComicReadingSourceSortField;
  direction?: SortDirection;
}

export function findByStatusIn(
  params: FindByStatusInParams,
  options?: { signal?: AbortSignal },
): Promise<ComicReadingSource[]> {
  const query = new URLSearchParams();
  params.statuses.forEach((status) => query.append('statuses', status));
  if (params.sortBy) query.set('sortBy', params.sortBy);
  if (params.direction) query.set('direction', params.direction);

  return apiFetch<ComicReadingSource[]>(`/reading-sources?${query.toString()}`, { signal: options?.signal });
}

export function findForModerationByStatusIn(
  params: FindByStatusInParams,
  options?: { signal?: AbortSignal },
): Promise<ComicReadingSourceModeration[]> {
  const query = new URLSearchParams();
  params.statuses.forEach((status) => query.append('statuses', status));
  if (params.sortBy) query.set('sortBy', params.sortBy);
  if (params.direction) query.set('direction', params.direction);

  return apiFetch<ComicReadingSourceModeration[]>(`/moderation/reading-sources?${query.toString()}`, {
    signal: options?.signal,
  });
}

export function search(
  sourceId: number,
  keywords: string,
  options?: { signal?: AbortSignal },
): Promise<ComicReadingSearchListing> {
  const query = new URLSearchParams({ keywords });

  return apiFetch<ComicReadingSearchListing>(`/reading-sources/${sourceId}/search?${query.toString()}`, {
    signal: options?.signal,
  });
}

export function findSearchResultDetails(
  sourceId: number,
  providerKey: string,
  externalId: string,
  options?: { signal?: AbortSignal },
): Promise<ComicReadingSearchResultDetails> {
  const query = new URLSearchParams({ providerKey, externalId });

  return apiFetch<ComicReadingSearchResultDetails>(
    `/reading-sources/${sourceId}/search-result-details?${query.toString()}`,
    { signal: options?.signal },
  );
}

export function approve(id: number): Promise<ComicReadingSource> {
  return apiFetch<ComicReadingSource>(`/reading-sources/${id}/approve`, { method: 'POST' });
}

export function reject(id: number): Promise<ComicReadingSource> {
  return apiFetch<ComicReadingSource>(`/reading-sources/${id}/reject`, { method: 'POST' });
}