import { useEffect, useMemo, useState } from 'react';
import { findByStatusIn } from '@/services/source/api/readingSource';
import type { ComicReadingSource } from '@/services/source/types';
import { searchEntryCandidates, type SourceSearchOutcome } from './searchEntryCandidates';

type SourceWithCandidates = Extract<SourceSearchOutcome, { kind: 'withCandidates' }>;

// The outcomes remember which keywords they belong to, so a new search starts from an empty list without having to
// reset state inside an effect (and without a render that still shows the previous search's results).
interface SearchProgress {
  forKeywords: string;
  outcomes: SourceSearchOutcome[];
}

/**
 * Searches `keywords` in every approved reading source, one request per source, and exposes what has arrived so
 * far. An empty `keywords` searches nothing. Changing `keywords` cancels the search in progress.
 */
export function useComicReadingEntryCandidateSearch(keywords: string) {
  const searchedKeywords = keywords.trim();

  const [sources, setSources] = useState<ComicReadingSource[]>([]);
  const [isLoadingSources, setIsLoadingSources] = useState(true);
  const [hasSourcesLoadError, setHasSourcesLoadError] = useState(false);
  const [progress, setProgress] = useState<SearchProgress>({ forKeywords: '', outcomes: [] });

  useEffect(() => {
    const controller = new AbortController();

    findByStatusIn({ statuses: ['APPROVED'] }, { signal: controller.signal })
      .then(setSources)
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return;
        }
        setHasSourcesLoadError(true);
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setIsLoadingSources(false);
        }
      });

    return () => {
      controller.abort();
    };
  }, []);

  useEffect(() => {
    if (searchedKeywords === '' || sources.length === 0) {
      return;
    }
    const controller = new AbortController();

    void searchEntryCandidates(
      sources,
      searchedKeywords,
      (outcome) =>
        setProgress((previous) => ({
          forKeywords: searchedKeywords,
          outcomes: previous.forKeywords === searchedKeywords ? [...previous.outcomes, outcome] : [outcome],
        })),
      controller.signal,
    );

    return () => {
      controller.abort();
    };
  }, [sources, searchedKeywords]);

  const outcomes = useMemo(
    () => (progress.forKeywords === searchedKeywords ? progress.outcomes : []),
    [progress, searchedKeywords],
  );

  const candidateGroups = useMemo(
    () => outcomes.filter((outcome): outcome is SourceWithCandidates => outcome.kind === 'withCandidates'),
    [outcomes],
  );
  const emptySources = useMemo(
    () => outcomes.filter((outcome) => outcome.kind === 'empty').map((outcome) => outcome.source),
    [outcomes],
  );
  const failedSources = useMemo(
    () => outcomes.filter((outcome) => outcome.kind === 'failed').map((outcome) => outcome.source),
    [outcomes],
  );
  const discardedCandidatesCount = outcomes.reduce(
    (total, outcome) => total + (outcome.kind === 'failed' ? 0 : outcome.discardedCandidatesCount),
    0,
  );

  const hasSearch = searchedKeywords !== '' && sources.length > 0;
  const sourcesToSearchCount = hasSearch ? sources.length : 0;
  const sourcesSettledCount = outcomes.length;

  return {
    isLoadingSources,
    hasSourcesLoadError,
    candidateGroups,
    emptySources,
    failedSources,
    discardedCandidatesCount,
    sourcesToSearchCount,
    sourcesSettledCount,
    isSearching: hasSearch && sourcesSettledCount < sourcesToSearchCount,
  };
}
