import { apiFetch } from '@/common/api/client';
import type { ComicReadingSource } from '../types';

export function findSelectable(options?: { signal?: AbortSignal }): Promise<ComicReadingSource[]> {
  return apiFetch<ComicReadingSource[]>('/reading-sources', { signal: options?.signal });
}