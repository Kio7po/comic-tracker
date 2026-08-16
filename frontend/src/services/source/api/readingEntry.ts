import { apiFetch } from '@/common/api/client';
import type { ComicReadingEntry, ComicReadingEntryRequest, ComicReadingEntryStatus } from '../types';

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