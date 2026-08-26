import { apiFetch } from '@/common/api/client';
import type { UserProfileRequest, UserResponse } from '../types';

export function me(options?: { signal?: AbortSignal }): Promise<UserResponse> {
  return apiFetch<UserResponse>('/users/me', { signal: options?.signal });
}

export function updateProfile(request: UserProfileRequest): Promise<UserResponse> {
  return apiFetch<UserResponse>('/users/me', { method: 'PUT', body: request });
}
