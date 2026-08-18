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
} as const;
