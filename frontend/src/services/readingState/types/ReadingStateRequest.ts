import type { ReadingStateStatus } from './ReadingStateStatus';

// notes/preferredEntryId are deliberately not optional (`?`) despite allowing `undefined` - PUT
// replaces the full ReadingState in one call, so a caller that forgets one of these would
// silently clear it. Making the key mandatory (even when the value is undefined) forces every
// call site to make an explicit choice instead of the compiler letting it slide.
export interface ReadingStateRequest {
  status: ReadingStateStatus;
  chapters: number;
  notes: string | undefined;
  preferredEntryId: number | undefined;
}
