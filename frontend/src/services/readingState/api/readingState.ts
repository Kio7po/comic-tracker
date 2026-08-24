import { apiFetch } from '@/common/api/client';
import type { ReadingState, ReadingStateRequest, ReadingStateWithComic } from '../types';

export function getByComic(slug: string, options?: { signal?: AbortSignal }): Promise<ReadingState> {
  return apiFetch<ReadingState>(`/comics/${encodeURIComponent(slug)}/reading-state`, { signal: options?.signal });
}

export function create(slug: string, request: ReadingStateRequest): Promise<ReadingState> {
  return apiFetch<ReadingState>(`/comics/${encodeURIComponent(slug)}/reading-state`, {
    method: 'POST',
    body: request,
  });
}

export function update(slug: string, request: ReadingStateRequest): Promise<ReadingState> {
  return apiFetch<ReadingState>(`/comics/${encodeURIComponent(slug)}/reading-state`, {
    method: 'PUT',
    body: request,
  });
}

export function remove(slug: string): Promise<void> {
  return apiFetch<void>(`/comics/${encodeURIComponent(slug)}/reading-state`, { method: 'DELETE' });
}

export function findByUser(options?: { signal?: AbortSignal }): Promise<ReadingStateWithComic[]> {
  return apiFetch<ReadingStateWithComic[]>('/reading-states', { signal: options?.signal });
}
