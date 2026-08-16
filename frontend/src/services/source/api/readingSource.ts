import { apiFetch } from '@/common/api/client';
import type { SortDirection } from '@/common/api/SortDirection';
import type { ComicReadingSource, ComicReadingSourceSortField, ComicReadingSourceStatus } from '../types';

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

export function approve(id: number): Promise<ComicReadingSource> {
  return apiFetch<ComicReadingSource>(`/reading-sources/${id}/approve`, { method: 'POST' });
}

export function reject(id: number): Promise<ComicReadingSource> {
  return apiFetch<ComicReadingSource>(`/reading-sources/${id}/reject`, { method: 'POST' });
}