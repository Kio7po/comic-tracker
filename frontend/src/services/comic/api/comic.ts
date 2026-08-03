import { apiFetch } from '@/common/api/client';
import type { Comic } from '../types';

export function getBySlug(slug: string): Promise<Comic> {
  return apiFetch<Comic>(`/comics/${encodeURIComponent(slug)}`);
}
