// Mirrors the backend's ProblemType constants (adapter/rest/exception/ProblemType.java).
// Lets callers branch on the specific problem instead of guessing it back from the HTTP
// status code, which is ambiguous whenever more than one exception maps to the same status.
export const ProblemType = {
  USERNAME_ALREADY_EXISTS: 'urn:problem-type:username-already-exists',
  EMAIL_ALREADY_EXISTS: 'urn:problem-type:email-already-exists',
  WEAK_PASSWORD: 'urn:problem-type:weak-password',
  INVALID_CREDENTIALS: 'urn:problem-type:invalid-credentials',
  INVALID_REFRESH_TOKEN: 'urn:problem-type:invalid-refresh-token',
  DUPLICATE_READING_SOURCE: 'urn:problem-type:duplicate-reading-source',
  DUPLICATE_READING_ENTRY: 'urn:problem-type:duplicate-reading-entry',
  READING_SOURCE_NOT_APPROVED: 'urn:problem-type:reading-source-not-approved',
  READING_SOURCE_ALREADY_REVIEWED: 'urn:problem-type:reading-source-already-reviewed',
  READING_SOURCE_NOT_FOUND: 'urn:problem-type:reading-source-not-found',
  READING_ENTRY_ALREADY_REVIEWED: 'urn:problem-type:reading-entry-already-reviewed',
  READING_ENTRY_NOT_FOUND: 'urn:problem-type:reading-entry-not-found',
  READING_STATE_NOT_FOUND: 'urn:problem-type:reading-state-not-found',
  READING_STATE_ALREADY_EXISTS: 'urn:problem-type:reading-state-already-exists',
  DATA_INTEGRITY_CONFLICT: 'urn:problem-type:data-integrity-conflict',
  READING_SOURCE_NOT_SUPPORTED: 'urn:problem-type:reading-source-not-supported',
  READING_PROVIDER_FAILED: 'urn:problem-type:reading-provider-failed',
  COMIC_NOT_FOUND_IN_READING_SOURCE: 'urn:problem-type:comic-not-found-in-reading-source',
  READING_COVER_UNAVAILABLE: 'urn:problem-type:reading-cover-unavailable',
} as const;
