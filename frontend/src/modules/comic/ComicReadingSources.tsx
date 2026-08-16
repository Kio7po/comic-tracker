import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation } from 'react-router';
import { ExternalLink, TriangleAlert } from 'lucide-react';
import { findByComic } from '@/services/source/api/readingEntry';
import type { ComicReadingEntry } from '@/services/source/types';
import { useAuth } from '@/common/components/AuthProvider';
import { Badge } from '@/common/components/ui/badge';
import { Card, CardContent } from '@/common/components/ui/card';
import { Checkbox } from '@/common/components/ui/checkbox';
import { Label } from '@/common/components/ui/label';
import { Spinner } from '@/common/components/ui/spinner';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/common/components/ui/tooltip';
import SuggestReadingSourceDialog from './SuggestReadingSourceDialog';

function languageLabel(locale: string): string {
  return locale.split('-')[0].toUpperCase();
}

function ComicReadingSources({ comicSlug }: Readonly<{ comicSlug: string }>) {
  const { t } = useTranslation();
  const { user, isLoading: isAuthLoading } = useAuth();
  const location = useLocation();

  const [entries, setEntries] = useState<ComicReadingEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [showPending, setShowPending] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoading(true);
    setHasError(false);
    findByComic(comicSlug, undefined, { signal: controller.signal })
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
  }, [comicSlug]);

  const visibleEntries = entries.filter(
    (entry) => entry.status === 'APPROVED' || (showPending && entry.status === 'PENDING'),
  );

  return (
    <Card>
      <CardContent>
        <div className="flex items-center justify-between gap-2">
          <h2 className="text-lg font-semibold text-foreground">{t('detail.readingSources')}</h2>
          {!isAuthLoading &&
            (user ? (
              <SuggestReadingSourceDialog
                comicSlug={comicSlug}
                onEntrySubmitted={(entry) => setEntries((previous) => [...previous, entry])}
              />
            ) : (
              <Link
                to="/login"
                state={{ from: location.pathname + location.search }}
                className="text-sm text-primary underline-offset-4 hover:underline"
              >
                {t('detail.suggestSource.loginPrompt')}
              </Link>
            ))}
        </div>
        <div className="mt-4 mb-6 flex items-center gap-2.5">
          <Checkbox
            id="show-pending-sources"
            checked={showPending}
            onCheckedChange={(checked) => setShowPending(checked === true)}
          />
          <Label htmlFor="show-pending-sources" className="text-sm font-normal text-muted-foreground">
            {t('detail.showPendingSources')}
          </Label>
        </div>
        {isLoading ? (
          <div className="mt-4 flex justify-center">
            <Spinner />
          </div>
        ) : hasError ? (
          <p className="mt-2 text-sm text-muted-foreground">{t('detail.readingSourcesError')}</p>
        ) : visibleEntries.length === 0 ? (
          <p className="mt-2 text-sm text-muted-foreground">{t('detail.readingSourcesEmpty')}</p>
        ) : (
          <ul className="mt-2 flex flex-col gap-2">
            {visibleEntries.map((entry) => (
              <li key={entry.id}>
                <a
                  href={entry.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-between rounded-md border border-border px-3 py-2 text-sm transition-colors hover:bg-muted"
                >
                  <span className="flex items-center gap-2 font-medium text-foreground">
                    {entry.source.iconUrl && (
                      <img src={entry.source.iconUrl} alt="" className="size-6 rounded-xs" />
                    )}
                    {entry.source.name}
                    {entry.status === 'PENDING' && (
                      <Tooltip>
                        <TooltipTrigger render={<span tabIndex={0} className="inline-flex text-amber-500" />}>
                          <TriangleAlert className="size-4" />
                        </TooltipTrigger>
                        <TooltipContent>{t('detail.pendingTooltip')}</TooltipContent>
                      </Tooltip>
                    )}
                  </span>
                  <span className="flex items-center gap-2 text-muted-foreground">
                    <Badge variant="outline">{languageLabel(entry.locale)}</Badge>
                    {entry.availableChapters !== null && (
                      <span>
                        {t('detail.chapters')} {entry.availableChapters}
                      </span>
                    )}
                    <ExternalLink className="size-4" />
                  </span>
                </a>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

export default ComicReadingSources;