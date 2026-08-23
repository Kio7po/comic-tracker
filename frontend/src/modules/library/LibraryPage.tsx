import { useMemo } from 'react';
import { useSearchParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import type { ComicMediaType, ComicStatus, LibraryComic, NsfwRating } from '@/services/comic/types';
import type { ReadingState, ReadingStateStatus } from '@/services/readingState/types';
import type { SortDirection } from '@/common/api/SortDirection';
import { matchesSearch } from '@/common/lib/matchesSearch';
import SearchBar from '@/common/components/SearchBar';
import LibraryComicCard from './LibraryComicCard';
import LibraryFilters, { SORT_FIELDS, type LibrarySortField } from './LibraryFilters';
import type { LibraryEntry } from './types';

const DEFAULT_SORT_FIELD: LibrarySortField = 'CREATED_AT';
const DEFAULT_SORT_DIRECTION: SortDirection = 'DESC';

// Client-side filtering over an in-memory array (see the mock data below) - a short debounce
// just smooths re-renders for fast typers, not throttling an expensive call like catalog's does.
const SEARCH_DEBOUNCE_MS = 150;

function mockComic(
  overrides: Pick<
    LibraryComic,
    'id' | 'slug' | 'title' | 'coverUrl' | 'alternativeTitles' | 'mediaType' | 'status' | 'nsfw' | 'chapters'
  > &
    Partial<Pick<LibraryComic, 'startDate'>>,
): LibraryComic {
  return { startDate: null, ...overrides };
}

function mockReadingState(
  overrides: Pick<ReadingState, 'id' | 'status' | 'chapters' | 'createdAt' | 'updatedAt'>,
): ReadingState {
  return { notes: null, ...overrides };
}

const COVER = 'https://cdn.myanimelist.net/images/manga/1/157897l.jpg';

// Temporary mock data for laying out the page - replace with a real findByUser() fetch
// once the visual design is settled.
const MOCK_ENTRIES: LibraryEntry[] = [
  {
    comic: mockComic({
      id: 1,
      slug: 'berserk',
      title: 'Berserk',
      alternativeTitles: ['Águila Negra'],
      coverUrl: COVER,
      mediaType: 'MANGA',
      status: 'ONGOING',
      nsfw: 'NONE',
      chapters: 364,
      startDate: '1989-08-25',
    }),
    readingState: mockReadingState({
      id: 1,
      status: 'READING',
      chapters: 340,
      createdAt: '2025-01-10T10:00:00Z',
      updatedAt: '2026-08-22T10:00:00Z',
    }),
  },
  {
    comic: mockComic({
      id: 2,
      slug: 'one-piece',
      title: 'One Piece',
      alternativeTitles: ['Búsqueda del Tesoro'],
      coverUrl: COVER,
      mediaType: 'MANGA',
      status: 'ONGOING',
      nsfw: 'NONE',
      chapters: null,
      startDate: '1997-07-22',
    }),
    readingState: mockReadingState({
      id: 2,
      status: 'READING',
      chapters: 0,
      createdAt: '2026-08-23T08:00:00Z',
      updatedAt: '2026-08-23T08:00:00Z',
    }),
  },
  {
    comic: mockComic({
      id: 3,
      slug: 'vagabond',
      title: 'Vagabond',
      alternativeTitles: ['El Camino Errático'],
      coverUrl: COVER,
      mediaType: 'MANGA',
      status: 'HIATUS',
      nsfw: 'NONE',
      chapters: 327,
      startDate: '1998-09-03',
    }),
    readingState: mockReadingState({
      id: 3,
      status: 'ON_HOLD',
      chapters: 200,
      createdAt: '2024-03-05T10:00:00Z',
      updatedAt: '2026-08-10T10:00:00Z',
    }),
  },
  {
    comic: mockComic({
      id: 4,
      slug: 'vinland-saga',
      title: 'Vinland Saga',
      alternativeTitles: ['Épica Vikinga'],
      coverUrl: COVER,
      mediaType: 'MANGA',
      status: 'ONGOING',
      nsfw: 'NONE',
      chapters: 216,
      startDate: '2005-04-13',
    }),
    readingState: mockReadingState({
      id: 4,
      status: 'COMPLETED',
      chapters: 216,
      createdAt: '2023-11-20T10:00:00Z',
      updatedAt: '2026-06-01T10:00:00Z',
    }),
  },
  {
    comic: mockComic({
      id: 5,
      slug: 'chainsaw-man',
      title: 'Chainsaw Man',
      alternativeTitles: ['Corazón de Motosierra'],
      coverUrl: COVER,
      mediaType: 'MANGA',
      status: 'ONGOING',
      nsfw: 'SUGGESTIVE',
      chapters: 150,
      startDate: '2018-12-03',
    }),
    readingState: mockReadingState({
      id: 5,
      status: 'PLAN_TO_READ',
      chapters: 0,
      createdAt: '2026-08-20T10:00:00Z',
      updatedAt: '2026-08-20T10:00:00Z',
    }),
  },
  {
    comic: mockComic({
      id: 6,
      slug: 'tokyo-ghoul',
      title: 'Tokyo Ghoul',
      alternativeTitles: ['Espíritu de Tokio'],
      coverUrl: COVER,
      mediaType: 'MANGA',
      status: 'COMPLETED',
      nsfw: 'NONE',
      chapters: 143,
      startDate: '2011-09-08',
    }),
    readingState: mockReadingState({
      id: 6,
      status: 'DROPPED',
      chapters: 45,
      createdAt: '2025-05-01T10:00:00Z',
      updatedAt: '2026-07-15T10:00:00Z',
    }),
  },
  {
    comic: mockComic({
      id: 7,
      slug: 'solo-leveling',
      title: 'Solo Leveling',
      alternativeTitles: ['Cazador Solitario'],
      coverUrl: COVER,
      mediaType: 'MANHWA',
      status: 'COMPLETED',
      nsfw: 'NONE',
      chapters: 200,
      startDate: '2018-03-04',
    }),
    readingState: mockReadingState({
      id: 7,
      status: 'READING',
      chapters: 150,
      createdAt: '2026-08-19T10:00:00Z',
      updatedAt: '2026-08-21T10:00:00Z',
    }),
  },
];

// Floored at 0: readingState.chapters (last chapter read) can legitimately end up ahead of
// comic.chapters (total known) if the comic's own chapter count is stale/under-reported.
function pendingChapters(entry: LibraryEntry): number {
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

function compareEntries(a: LibraryEntry, b: LibraryEntry, field: LibrarySortField): number {
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

function sortEntries(entries: LibraryEntry[], field: LibrarySortField, direction: SortDirection): LibraryEntry[] {
  const multiplier = direction === 'DESC' ? -1 : 1;
  return [...entries].sort((a, b) => multiplier * compareEntries(a, b, field));
}

// Ceiling filter, same semantics as catalog's own nsfw filter: an entry passes if its rating
// is at or below the selected one, not only on an exact match. Unrated (null) comics are
// treated as NONE.
const NSFW_RANK: Record<NsfwRating, number> = { NONE: 0, SUGGESTIVE: 1, EXPLICIT: 2 };

function LibraryPage() {
  const { t } = useTranslation();
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

  const entries = useMemo(() => {
    const filtered = MOCK_ENTRIES.filter((entry) => {
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
  }, [query, readingStatus, pendingOnly, mediaType, publicationStatus, nsfw, sortField, sortDirection]);

  return (
    <div className="mx-auto max-w-6xl px-6 py-8">
      <h1 className="mb-4 text-2xl font-semibold text-foreground">{t('library.title')}</h1>
      <SearchBar
        value={query}
        onChange={handleQueryChange}
        placeholder={t('library.searchPlaceholder')}
        debounceMs={SEARCH_DEBOUNCE_MS}
      />
      <LibraryFilters
        readingStatus={readingStatus}
        pendingOnly={pendingOnly}
        mediaType={mediaType}
        publicationStatus={publicationStatus}
        nsfw={nsfw}
        sortField={sortField}
        sortDirection={sortDirection}
        onReadingStatusChange={handleReadingStatusChange}
        onPendingOnlyChange={handlePendingOnlyChange}
        onMediaTypeChange={handleMediaTypeChange}
        onPublicationStatusChange={handlePublicationStatusChange}
        onNsfwChange={handleNsfwChange}
        onSortFieldChange={handleSortFieldChange}
        onSortDirectionChange={handleSortDirectionChange}
      />
      {entries.length === 0 ? (
        <p className="mt-6 text-sm text-muted-foreground">{t('library.empty')}</p>
      ) : (
        <div className="mt-6 grid grid-cols-3 gap-4 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6">
          {entries.map((entry) => (
            <LibraryComicCard key={entry.comic.slug} entry={entry} />
          ))}
        </div>
      )}
    </div>
  );
}

export default LibraryPage;
