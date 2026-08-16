import { apiFetch } from '@/common/api/client';
import type { PageResponse } from '@/common/api/PageResponse';
import type { SortDirection } from '@/common/api/SortDirection';
import type {
  ComicReadingEntry,
  ComicReadingEntryModeration,
  ComicReadingEntryRequest,
  ComicReadingEntrySortField,
  ComicReadingEntryStatus,
} from '../types';

export function findByComic(
  slug: string,
  params?: { status?: ComicReadingEntryStatus },
  options?: { signal?: AbortSignal },
): Promise<ComicReadingEntry[]> {
  const query = new URLSearchParams();
  if (params?.status) query.set('status', params.status);
  const queryString = query.toString();
  const path = `/comics/${encodeURIComponent(slug)}/reading-entries` + (queryString ? `?${queryString}` : '');

  return apiFetch<ComicReadingEntry[]>(path, { signal: options?.signal });
}

export function submit(slug: string, request: ComicReadingEntryRequest): Promise<ComicReadingEntry> {
  return apiFetch<ComicReadingEntry>(`/comics/${encodeURIComponent(slug)}/reading-entries`, {
    method: 'POST',
    body: request,
  });
}

export interface FindEntriesByStatusInParams {
  statuses: ComicReadingEntryStatus[];
  sortBy?: ComicReadingEntrySortField;
  direction?: SortDirection;
  limit?: number;
  offset?: number;
}

export function findByStatusIn(
  params: FindEntriesByStatusInParams,
  options?: { signal?: AbortSignal },
): Promise<PageResponse<ComicReadingEntryModeration>> {
  const query = new URLSearchParams();
  params.statuses.forEach((status) => query.append('statuses', status));
  if (params.sortBy) query.set('sortBy', params.sortBy);
  if (params.direction) query.set('direction', params.direction);
  if (params.limit !== undefined) query.set('limit', String(params.limit));
  if (params.offset !== undefined) query.set('offset', String(params.offset));

  return apiFetch<PageResponse<ComicReadingEntryModeration>>(`/reading-entries?${query.toString()}`, {
    signal: options?.signal,
  });
}

export function approve(id: number): Promise<ComicReadingEntry> {
  return apiFetch<ComicReadingEntry>(`/reading-entries/${id}/approve`, { method: 'POST' });
}

export function reject(id: number): Promise<ComicReadingEntry> {
  return apiFetch<ComicReadingEntry>(`/reading-entries/${id}/reject`, { method: 'POST' });
}