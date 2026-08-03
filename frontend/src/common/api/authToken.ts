// In-memory only, never localStorage/sessionStorage due to XSS-exposure.
// Same reasoning as the HttpOnly refresh cookie.
let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}
