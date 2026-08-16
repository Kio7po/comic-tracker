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
} as const;
