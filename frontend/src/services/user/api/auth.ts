import { apiFetch, type TokenResponse } from '@/common/api/client';
import { setAccessToken } from '@/common/api/authToken';
import type { RegisterRequest, LoginRequest, UserResponse } from '../types';

export function register(request: RegisterRequest): Promise<UserResponse> {
  return apiFetch<UserResponse>('/auth/register', { method: 'POST', body: request });
}

export async function login(request: LoginRequest): Promise<void> {
  const tokens = await apiFetch<TokenResponse>('/auth/login', { method: 'POST', body: request });
  setAccessToken(tokens.access_token);
}

export function me(options?: { signal?: AbortSignal }): Promise<UserResponse> {
  return apiFetch<UserResponse>('/auth/me', { signal: options?.signal });
}

export async function logout(): Promise<void> {
  await apiFetch<void>('/auth/logout', { method: 'POST' });
  setAccessToken(null);
}
