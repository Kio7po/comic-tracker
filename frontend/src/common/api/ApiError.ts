export interface ProblemDetailBody {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
}

// Mirrors the backend's RFC 9457 ProblemDetail response shape (see GlobalExceptionHandler).
export class ApiError extends Error {
  readonly status: number;
  readonly type?: string;
  readonly title?: string;
  readonly instance?: string;

  constructor(status: number, problem: ProblemDetailBody | null) {
    super(problem?.detail ?? `Request failed with status ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.type = problem?.type;
    this.title = problem?.title;
    this.instance = problem?.instance;
  }
}
