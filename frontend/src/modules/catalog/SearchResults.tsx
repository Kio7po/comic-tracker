import { useTranslation } from 'react-i18next';
import ComicCard from './ComicCard';
import { Spinner } from '@/common/components/ui/spinner';
import type { ComicSearchResult } from '@/services/comic/types';

interface SearchResultsProps {
  results: ComicSearchResult[];
  isLoading: boolean;
  hasError: boolean;
}

function SearchResults({ results, isLoading, hasError }: Readonly<SearchResultsProps>) {
  const { t } = useTranslation();

  if (isLoading) {
    return (
      <div className="mt-6 flex justify-center">
        <Spinner className="size-6" aria-label={t('catalog.loading')} />
      </div>
    );
  }

  if (hasError) {
    return <p className="mt-6 text-center text-sm text-destructive">{t('catalog.loadError')}</p>;
  }

  if (results.length === 0) {
    return <p className="mt-6 text-center text-sm text-muted-foreground">{t('catalog.noResults')}</p>;
  }

  return (
    <div className="mt-6 grid grid-cols-3 gap-4 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6">
      {results.map((comic) => (
        <ComicCard key={`${comic.sourceSlug}-${comic.externalId}`} comic={comic} />
      ))}
    </div>
  );
}

export default SearchResults;
