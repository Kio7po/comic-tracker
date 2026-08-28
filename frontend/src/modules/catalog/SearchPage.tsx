import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { search } from '@/services/comic/api/catalog';
import type { SortDirection } from '@/common/api/SortDirection';
import type {
  ComicMediaType,
  ComicSearchResult,
  ComicSearchSortField,
  ComicStatus,
  NsfwRating,
} from '@/services/comic/types';
import SearchBar from '@/common/components/SearchBar';
import SearchFilters, { LIMIT_OPTIONS, SORT_FIELDS } from './SearchFilters';
import SearchResults from './SearchResults';
import SearchPagination from './SearchPagination';

const DEFAULT_LIMIT = 24;
const DEFAULT_SORT_FIELD: ComicSearchSortField = 'RELEVANCE';
const DEFAULT_SORT_DIRECTION: SortDirection = 'DESC';

function SearchPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();

  const page = Math.max(1, Number(searchParams.get('page')) || 1);
  const keywords = searchParams.get('keywords') ?? '';
  const type = (searchParams.get('type') as ComicMediaType | null) ?? undefined;
  const status = (searchParams.get('status') as ComicStatus | null) ?? undefined;
  const nsfw = (searchParams.get('nsfw') as NsfwRating | null) ?? undefined;
  const requestedLimit = Number(searchParams.get('limit'));
  const limit = LIMIT_OPTIONS.includes(requestedLimit) ? requestedLimit : DEFAULT_LIMIT;
  const requestedSortBy = searchParams.get('sortBy');
  const sortBy = SORT_FIELDS.includes(requestedSortBy as ComicSearchSortField)
    ? (requestedSortBy as ComicSearchSortField)
    : DEFAULT_SORT_FIELD;
  const direction: SortDirection = searchParams.get('direction') === 'ASC' ? 'ASC' : DEFAULT_SORT_DIRECTION;

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

  function handlePageChange(newPage: number) {
    updateParams({ page: newPage === 1 ? null : String(newPage) });
  }

  function handleKeywordsChange(value: string) {
    updateParams({ keywords: value || null, page: null });
  }

  function handleTypeChange(value: ComicMediaType | undefined) {
    updateParams({ type: value ?? null, page: null });
  }

  function handleStatusChange(value: ComicStatus | undefined) {
    updateParams({ status: value ?? null, page: null });
  }

  function handleNsfwChange(value: NsfwRating | undefined) {
    updateParams({ nsfw: value ?? null, page: null });
  }

  function handleLimitChange(value: number) {
    updateParams({ limit: value === DEFAULT_LIMIT ? null : String(value), page: null });
  }

  function handleSortByChange(value: ComicSearchSortField) {
    updateParams({ sortBy: value === DEFAULT_SORT_FIELD ? null : value, page: null });
  }

  function handleDirectionChange(value: SortDirection) {
    updateParams({ direction: value === DEFAULT_SORT_DIRECTION ? null : value, page: null });
  }

  const [results, setResults] = useState<ComicSearchResult[]>([]);
  const [totalItems, setTotalItems] = useState<number | null>(null);
  const [existMoreItems, setExistMoreItems] = useState(true);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    // Reset loading/error before each new fetch; safe here since the effect
    // is keyed on the search params and cleanup guards against stale updates.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoading(true);
    setHasError(false);
    search(
      {
        keywords,
        limit,
        offset: (page - 1) * limit,
        type,
        status,
        nsfw,
        sortBy,
        // Direction has no effect server-side when sortBy is RELEVANCE - omit it too, rather
        // than sending a value that would just be ignored.
        direction: sortBy === 'RELEVANCE' ? undefined : direction,
      },
      { signal: controller.signal },
    )
      .then((response) => {
        setResults(response.items);
        setTotalItems(response.totalItems);
        setExistMoreItems(response.existMoreItems);
      })
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
  }, [page, keywords, type, status, nsfw, limit, sortBy, direction]);

  const totalPages = totalItems !== null ? Math.max(1, Math.ceil(totalItems / limit)) : null;

  return (
    <div className="mx-auto max-w-6xl px-6 py-8">
      <SearchBar value={keywords} onChange={handleKeywordsChange} placeholder={t('catalog.searchPlaceholder')} />
      <SearchFilters
        type={type}
        status={status}
        nsfw={nsfw}
        limit={limit}
        sortBy={sortBy}
        direction={direction}
        onTypeChange={handleTypeChange}
        onStatusChange={handleStatusChange}
        onNsfwChange={handleNsfwChange}
        onLimitChange={handleLimitChange}
        onSortByChange={handleSortByChange}
        onDirectionChange={handleDirectionChange}
      />
      <SearchPagination
        page={page}
        onPageChange={handlePageChange}
        totalPages={totalPages}
        existMoreItems={existMoreItems}
        className="mt-4 md:justify-end justify-center"
      />
      <SearchResults results={results} isLoading={isLoading} hasError={hasError} />
      <SearchPagination
        page={page}
        onPageChange={handlePageChange}
        totalPages={totalPages}
        existMoreItems={existMoreItems}
      />
    </div>
  );
}

export default SearchPage;
