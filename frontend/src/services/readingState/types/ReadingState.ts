import type { ReadingStateStatus } from './ReadingStateStatus';

export interface ReadingState {
  id: number;
  status: ReadingStateStatus;
  chapters: number;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}
