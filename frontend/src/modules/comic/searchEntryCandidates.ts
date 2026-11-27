import { search } from '@/services/source/api/readingSource';
import type { ComicReadingSource } from '@/services/source/types';
import { ApiError, ProblemType } from '@/common/api';
import { runWithConcurrencyLimit } from '@/common/lib/runWithConcurrencyLimit';

// The browser (HTTP/1.1, which is what the dev proxy gives) already queues around 6 connections per host; with
// HTTP/2 it wouldn't, so the cap lives here too to protect the backend and Suwayomi when sources grow in number.
// This is not real protection and the backend should limit it by itself with HTTP/2
export const MAX_CONCURRENT_SOURCE_SEARCHES = 6;

/** A search result that can become an entry: unlike a raw result, it always has the id the provider issued. */
export interface EntryCandidate {
  externalId: string;
  title: string;
  url: string | null;
  coverUrl: string | null;
}

export type SourceSearchOutcome =
  | {
      kind: 'withCandidates';
      source: ComicReadingSource;
      // The provider that ran the search: the candidates' ids only mean something to it.
      providerKey: string;
      candidates: EntryCandidate[];
      discardedCandidatesCount: number;
    }
  | { kind: 'empty'; source: ComicReadingSource; discardedCandidatesCount: number }
  | { kind: 'failed'; source: ComicReadingSource };

async function searchSource(
  source: ComicReadingSource,
  keywords: string,
  signal: AbortSignal,
): Promise<SourceSearchOutcome> {
  try {
    const listing = await search(source.id, keywords, { signal });
    const candidates: EntryCandidate[] = [];
    for (const result of listing.results) {
      // Without the provider's id there is no way to ask it for this work, so it can't become an entry.
      if (result.externalId !== null) {
        candidates.push({
          externalId: result.externalId,
          title: result.title,
          url: result.url,
          coverUrl: result.coverUrl,
        });
      }
    }
    const discardedCandidatesCount = listing.results.length - candidates.length;
    if (candidates.length === 0) {
      return { kind: 'empty', source, discardedCandidatesCount };
    }
    return { kind: 'withCandidates', source, providerKey: listing.providerKey, candidates, discardedCandidatesCount };
  } catch (error) {
    // No provider serves this source: nothing broke, it simply can't give candidates.
    if (error instanceof ApiError && error.type === ProblemType.READING_SOURCE_NOT_SUPPORTED) {
      return { kind: 'empty', source, discardedCandidatesCount: 0 };
    }
    throw error;
  }
}

/**
 * Searches `keywords` in every source, one request per source with at most
 * `MAX_CONCURRENT_SOURCE_SEARCHES` in flight, and calls `onSourceSettled` as each one finishes (in the order they
 * finish, not the order of `sources`).
 *
 * A source that fails is reported as `failed` and doesn't affect the others. Once `signal` aborts, nothing more is
 * started and nothing more is reported, including the answers of requests already in flight.
 */
export async function searchEntryCandidates(
  sources: readonly ComicReadingSource[],
  keywords: string,
  onSourceSettled: (outcome: SourceSearchOutcome) => void,
  signal: AbortSignal,
): Promise<void> {
  await runWithConcurrencyLimit(
    sources,
    MAX_CONCURRENT_SOURCE_SEARCHES,
    async (source) => {
      let outcome: SourceSearchOutcome;
      try {
        outcome = await searchSource(source, keywords, signal);
      } catch {
        outcome = { kind: 'failed', source };
      }
      if (!signal.aborted) {
        onSourceSettled(outcome);
      }
    },
    signal,
  );
}
