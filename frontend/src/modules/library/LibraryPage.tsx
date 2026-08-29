import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { SlidersHorizontal } from 'lucide-react';
import type { ComicMediaType, ComicStatus, NsfwRating } from '@/services/comic/types';
import { findByUser } from '@/services/readingState/api/readingState';
import type { ReadingState, ReadingStateStatus, ReadingStateWithComic } from '@/services/readingState/types';
import type { SortDirection } from '@/common/api/SortDirection';
import { matchesSearch } from '@/common/lib/matchesSearch';
import { MOBILE_QUERY, useMediaQuery } from '@/common/hooks/useMediaQuery';
import SearchBar from '@/common/components/SearchBar';
import { Button } from '@/common/components/ui/button';
import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
  DrawerTrigger,
} from '@/common/components/ui/drawer';
import { Spinner } from '@/common/components/ui/spinner';
import LibraryComicCard from './LibraryComicCard';
import LibraryFilters, { SORT_FIELDS, type LibrarySortField } from './LibraryFilters';

const DEFAULT_SORT_FIELD: LibrarySortField = 'CREATED_AT';
const DEFAULT_SORT_DIRECTION: SortDirection = 'DESC';

// Client-side filtering/sorting over the full findByUser() result - not paginated, matches the
// original design ("no estará paginado"). A short debounce just smooths re-renders for fast
// typers, not throttling an expensive call like catalog's does.
const SEARCH_DEBOUNCE_MS = 250;

// Floored at 0: readingState.chapters (last chapter read) can legitimately end up ahead of
// comic.chapters (total known) if the comic's own chapter count is stale/under-reported.
function pendingChapters(entry: ReadingStateWithComic): number {
  return entry.comic.chapters !== null ? Math.max(0, entry.comic.chapters - entry.readingState.chapters) : 0;
}

// Nullable fields (RELEASE_DATE, TOTAL_CHAPTERS) sort unknown values to the end in ascending
// order (and so to the start once the direction multiplier flips it for descending).
function compareNullable<T>(a: T | null, b: T | null, compare: (a: T, b: T) => number): number {
  if (a === null && b === null) return 0;
  if (a === null) return 1;
  if (b === null) return -1;
  return compare(a, b);
}

function compareEntries(a: ReadingStateWithComic, b: ReadingStateWithComic, field: LibrarySortField): number {
  switch (field) {
    case 'TITLE':
      return a.comic.title.localeCompare(b.comic.title);
    case 'RELEASE_DATE':
      return compareNullable(a.comic.startDate, b.comic.startDate, (x, y) => x.localeCompare(y));
    case 'TOTAL_CHAPTERS':
      return compareNullable(a.comic.chapters, b.comic.chapters, (x, y) => x - y);
    case 'UNREAD_CHAPTERS':
      return pendingChapters(a) - pendingChapters(b);
    case 'CREATED_AT':
      return a.readingState.createdAt.localeCompare(b.readingState.createdAt);
    case 'UPDATED_AT':
      return a.readingState.updatedAt.localeCompare(b.readingState.updatedAt);
  }
}

function sortEntries(entries: ReadingStateWithComic[], field: LibrarySortField, direction: SortDirection): ReadingStateWithComic[] {
  const multiplier = direction === 'DESC' ? -1 : 1;
  return [...entries].sort((a, b) => multiplier * compareEntries(a, b, field));
}

// Ceiling filter, same semantics as catalog's own nsfw filter: an entry passes if its rating
// is at or below the selected one, not only on an exact match. Unrated (null) comics are
// treated as NONE.
const NSFW_RANK: Record<NsfwRating, number> = { NONE: 0, SUGGESTIVE: 1, EXPLICIT: 2 };

function LibraryPage() {
  const { t } = useTranslation();
  const isMobile = useMediaQuery(MOBILE_QUERY);
  const [searchParams, setSearchParams] = useSearchParams();

  const query = searchParams.get('q') ?? '';
  const readingStatus = (searchParams.get('read') as ReadingStateStatus | null) ?? undefined;
  const pendingOnly = searchParams.get('pending') === 'true';
  const mediaType = (searchParams.get('type') as ComicMediaType | null) ?? undefined;
  const publicationStatus = (searchParams.get('pub') as ComicStatus | null) ?? undefined;
  const nsfw = (searchParams.get('nsfw') as NsfwRating | null) ?? undefined;
  const requestedSortField = searchParams.get('sort');
  const sortField = SORT_FIELDS.includes(requestedSortField as LibrarySortField)
    ? (requestedSortField as LibrarySortField)
    : DEFAULT_SORT_FIELD;
  const sortDirection: SortDirection = searchParams.get('dir') === 'ASC' ? 'ASC' : DEFAULT_SORT_DIRECTION;

  function updateParams(updates: Record<string, string | null>) {
    setSearchParams((previous) => {
      const next = new URLSearchParams(previous);
      for (const [key, value] of Object.entries(updates)) {
        if (value === null) {
          next.delete(key);
        } else {
          next.set(key, value);
        }
      }
      return next;
    });
  }

  function handleQueryChange(value: string) {
    updateParams({ q: value || null });
  }

  function handleReadingStatusChange(value: ReadingStateStatus | undefined) {
    updateParams({ read: value ?? null });
  }

  function handlePendingOnlyChange(value: boolean) {
    updateParams({ pending: value ? 'true' : null });
  }

  function handleMediaTypeChange(value: ComicMediaType | undefined) {
    updateParams({ type: value ?? null });
  }

  function handlePublicationStatusChange(value: ComicStatus | undefined) {
    updateParams({ pub: value ?? null });
  }

  function handleNsfwChange(value: NsfwRating | undefined) {
    updateParams({ nsfw: value ?? null });
  }

  function handleSortFieldChange(value: LibrarySortField) {
    updateParams({ sort: value === DEFAULT_SORT_FIELD ? null : value });
  }

  function handleSortDirectionChange(value: SortDirection) {
    updateParams({ dir: value === DEFAULT_SORT_DIRECTION ? null : value });
  }

  function handleResetFilters() {
    updateParams({ q: null, read: null, pending: null, type: null, pub: null, nsfw: null, sort: null, dir: null });
  }

  const isAtDefault =
    query.trim() === '' &&
    readingStatus === undefined &&
    !pendingOnly &&
    mediaType === undefined &&
    publicationStatus === undefined &&
    nsfw === undefined &&
    sortField === DEFAULT_SORT_FIELD &&
    sortDirection === DEFAULT_SORT_DIRECTION;

  const [entries, setEntries] = useState<ReadingStateWithComic[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoading(true);
    setHasError(false);
    findByUser({ signal: controller.signal })
      .then(setEntries)
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return;
        }
        setHasError(true);
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      });

    return () => {
      controller.abort();
    };
  }, []);

  const handleEntryUpdated = useCallback((comicSlug: string, readingState: ReadingState) => {
    setEntries((previous) =>
      previous.map((entry) => (entry.comic.slug === comicSlug ? { ...entry, readingState } : entry)),
    );
  }, []);

  const handleEntryRemoved = useCallback((comicSlug: string) => {
    setEntries((previous) => previous.filter((entry) => entry.comic.slug !== comicSlug));
  }, []);

  const filteredEntries = useMemo(() => {
    const filtered = entries.filter((entry) => {
      const titles = [entry.comic.title, ...entry.comic.alternativeTitles];
      if (!matchesSearch(titles, query)) return false;
      if (readingStatus && entry.readingState.status !== readingStatus) return false;
      if (pendingOnly && pendingChapters(entry) <= 0) return false;
      if (mediaType && entry.comic.mediaType !== mediaType) return false;
      if (publicationStatus && entry.comic.status !== publicationStatus) return false;
      if (nsfw && NSFW_RANK[entry.comic.nsfw ?? 'NONE'] > NSFW_RANK[nsfw]) return false;
      return true;
    });
    return sortEntries(filtered, sortField, sortDirection);
  }, [entries, query, readingStatus, pendingOnly, mediaType, publicationStatus, nsfw, sortField, sortDirection]);

  const filtersProps = {
    readingStatus,
    pendingOnly,
    mediaType,
    publicationStatus,
    nsfw,
    sortField,
    sortDirection,
    onReadingStatusChange: handleReadingStatusChange,
    onPendingOnlyChange: handlePendingOnlyChange,
    onMediaTypeChange: handleMediaTypeChange,
    onPublicationStatusChange: handlePublicationStatusChange,
    onNsfwChange: handleNsfwChange,
    onSortFieldChange: handleSortFieldChange,
    onSortDirectionChange: handleSortDirectionChange,
    isAtDefault,
    onReset: handleResetFilters,
  };

  function renderFilters() {
    if (isMobile) {
      return (
        <Drawer>
          <DrawerTrigger render={<Button type="button" variant="outline" className="mt-3" />}>
            <SlidersHorizontal className="size-4" />
            {t('library.filters.trigger')}
          </DrawerTrigger>
          <DrawerContent>
            <DrawerHeader>
              <DrawerTitle>{t('library.filters.trigger')}</DrawerTitle>
            </DrawerHeader>
            <div className="overflow-y-auto px-4 pb-4">
              <LibraryFilters {...filtersProps} alignSelectWithTrigger={false} />
            </div>
            <DrawerFooter>
              <DrawerClose render={<Button type="button" size="lg" variant="outline" />}>
                {t('common.close')}
              </DrawerClose>
            </DrawerFooter>
          </DrawerContent>
        </Drawer>
      );
    }

    return <LibraryFilters {...filtersProps} />;
  }

  function renderEntries() {
    if (isLoading) {
      return (
        <div className="mt-6 flex justify-center">
          <Spinner className="size-6" aria-label={t('catalog.loading')} />
        </div>
      );
    }

    if (hasError) {
      return <p className="mt-6 text-center text-sm text-destructive">{t('library.loadError')}</p>;
    }

    if (filteredEntries.length === 0) {
      const emptyMessage = entries.length > 0 ? t('library.filteredEmpty') : t('library.empty');
      return <p className="mt-6 text-sm text-muted-foreground">{emptyMessage}</p>;
    }

    return (
      <div className="mt-6 grid grid-cols-3 gap-4 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6">
        {filteredEntries.map((entry) => (
          <LibraryComicCard
            key={entry.comic.slug}
            entry={entry}
            onUpdated={handleEntryUpdated}
            onRemoved={handleEntryRemoved}
          />
        ))}
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-6 py-8">
      <h1 className="mb-4 text-2xl font-semibold text-foreground">{t('library.title')}</h1>
      <SearchBar
        value={query}
        onChange={handleQueryChange}
        placeholder={t('library.searchPlaceholder')}
        debounceMs={SEARCH_DEBOUNCE_MS}
      />
      {renderFilters()}
      {renderEntries()}
    </div>
  );
}

export default LibraryPage;
