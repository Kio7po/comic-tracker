// Single mechanism for "return to where the user came from" after login, usable
// from a loader-thrown redirect() (which, unlike a <Link>, can't carry router `state`)
export function appendFromParam(path: string, from: string | null | undefined): string {
  return from ? `${path}?from=${encodeURIComponent(from)}` : path;
}