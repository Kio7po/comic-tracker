import type { ReadingStateStatus } from './ReadingStateStatus';

export interface ReadingStateRequest {
  status: ReadingStateStatus;
  chapters: number;
  notes?: string;
}
