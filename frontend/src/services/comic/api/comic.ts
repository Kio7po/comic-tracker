import { apiFetch } from '@/common/api/client';
import type { Comic } from '../types';

export function getBySlug(slug: string, options?: { signal?: AbortSignal }): Promise<Comic> {
  return apiFetch<Comic>(`/comics/${encodeURIComponent(slug)}`, { signal: options?.signal });
}
