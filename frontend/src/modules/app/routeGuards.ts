import { redirect } from 'react-router';
import { me } from '@/services/user/api/user';
import { ApiError } from '@/common/api/ApiError';
import { appendFromParam } from '@/common/lib/authRedirect';
import type { UserResponse, UserRole } from '@/services/user/types';

/**
 * Loader guard: redirects to /login (preserving the current URL as ?from=) when there's no
 * session. Runs outside the component tree, so it checks the session itself instead of using
 * useAuth().
 */
export async function requireAuth(request: Request): Promise<UserResponse> {
  try {
    return await me({ signal: request.signal });
  } catch (error) {
    // apiFetch already tried refresh-and-retry internally; a 401 surfacing here means that also
    // failed, i.e. there's genuinely no session. Any other failure (network error, 500...) is a
    // different problem and shouldn't be masked behind a misleading "please log in" redirect.
    if (error instanceof ApiError && error.status === 401) {
      const url = new URL(request.url);
      throw redirect(appendFromParam('/login', url.pathname + url.search));
    }
    throw error;
  }
}

/**
 * Loader guard: same as requireAuth, but also throws a 403 Response when the session doesn't
 * have the required role.
 */
export async function requireRole(role: UserRole, request: Request): Promise<UserResponse> {
  const user = await requireAuth(request);

  if (user.role !== role) {
    throw new Response('Forbidden', { status: 403 });
  }

  return user;
}