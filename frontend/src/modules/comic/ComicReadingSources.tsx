import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation } from 'react-router';
import { ExternalLink as ExternalLinkIcon, TriangleAlert } from 'lucide-react';
import { findByComic } from '@/services/source/api/readingEntry';
import type { ComicReadingEntry } from '@/services/source/types';
import { useAuth } from '@/common/components/AuthProvider';
import ExternalLink from '@/common/components/ExternalLink';
import TruncatedText from '@/common/components/TruncatedText';
import { appendFromParam } from '@/common/lib/authRedirect';
import { createLanguageNameFormatter } from '@/common/lib/languageName';
import { useLocalStorage } from '@/common/hooks/useLocalStorage';
import { Badge } from '@/common/components/ui/badge';
import { Card, CardContent } from '@/common/components/ui/card';
import { Checkbox } from '@/common/components/ui/checkbox';
import { Label } from '@/common/components/ui/label';
import { Spinner } from '@/common/components/ui/spinner';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/common/components/ui/tooltip';
import ReadingSourceLocaleFilter from './ReadingSourceLocaleFilter';
import SuggestReadingSourceDialog from './SuggestReadingSourceDialog';

function languageLabel(locale: string): string {
  return locale.split('-')[0].toUpperCase();
}

function ComicReadingSources({ comicSlug }: Readonly<{ comicSlug: string }>) {
  const { t, i18n } = useTranslation();
  const { user, isLoading: isAuthLoading } = useAuth();
  const location = useLocation();

  const [entries, setEntries] = useState<ComicReadingEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [showPending, setShowPending] = useLocalStorage('showPendingReadingSources', false);
  // Defaults to the user's preferred reading locale so the list opens
  // already narrowed to it; empty means "no filter, show every language".
  // If auth is still resolving at mount, this just misses the default and starts
  // unfiltered instead, safe direction to fail in, since it shows more, not less.
  const [selectedLocaleCodes, setSelectedLocaleCodes] = useState<string[]>(() =>
    user?.locale ? [user.locale] : [],
  );

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

  // Only offers languages actually present among this comic's sources - showing the full
  // curated language list (see SuggestReadingSourceDialog) would let the user pick one that
  // filters everything out.
  const localeOptions = useMemo(() => {
    const nameOf = createLanguageNameFormatter(i18n.language);
    const distinctLocales = Array.from(new Set(entries.map((entry) => entry.locale)));
    return distinctLocales
      .map((code) => ({ value: code, label: nameOf(code) }))
      .sort((a, b) => a.label.localeCompare(b.label, i18n.language));
  }, [entries, i18n.language]);
  // selectedLocaleCodes can hold a code that isn't (or no longer is) one of localeOptions - e.g.
  // the user's default locale has no source in it. Everything (the chips shown, the filtering
  // applied, the reset button) has to agree on the same effective selection, the intersection
  // with what's actually available, or they drift out of sync with each other, like the chips
  // showing "nothing selected" while entries are still being filtered down to zero.
  const selectedLocaleOptions = localeOptions.filter((option) => selectedLocaleCodes.includes(option.value));
  const effectiveLocaleCodes = selectedLocaleOptions.map((option) => option.value);
  const defaultLocaleCodes =
    user?.locale && localeOptions.some((option) => option.value === user.locale) ? [user.locale] : [];
  const isLocaleFilterAtDefault =
    effectiveLocaleCodes.length === defaultLocaleCodes.length &&
    defaultLocaleCodes.every((code) => effectiveLocaleCodes.includes(code));

  const visibleEntries = entries.filter(
    (entry) =>
      (entry.status === 'APPROVED' || (showPending && entry.status === 'PENDING')) &&
      (effectiveLocaleCodes.length === 0 || effectiveLocaleCodes.includes(entry.locale)),
  );

  function renderList() {
    if (isLoading) {
      return (
        <div className="mt-4 flex justify-center">
          <Spinner />
        </div>
      );
    }
    if (hasError) {
      return <p className="mt-2 text-sm text-muted-foreground">{t('detail.readingSourcesError')}</p>;
    }
    if (visibleEntries.length === 0) {
      const emptyMessage = entries.length > 0 ? t('detail.readingSourcesFilteredEmpty') : t('detail.readingSourcesEmpty');
      return <p className="mt-2 text-sm text-muted-foreground">{emptyMessage}</p>;
    }
    return (
      <ul className="mt-2 flex flex-col gap-2">
        {visibleEntries.map((entry) => (
          <li key={entry.id}>
            <ExternalLink
              href={entry.url}
              className="flex items-center justify-between gap-3 rounded-md border border-border px-3 py-2 text-sm transition-colors hover:bg-muted"
            >
              <span className="flex min-w-0 items-center gap-2">
                {entry.source.iconUrl && (
                  <img src={entry.source.iconUrl} alt="" className="size-8 shrink-0 rounded-md" />
                )}
                <span className="flex min-w-0 flex-col">
                  <span className="flex min-w-0 items-center gap-2 font-medium text-foreground">
                    <TruncatedText text={entry.source.name} />
                    {entry.status === 'PENDING' && (
                      <Tooltip>
                        <TooltipTrigger
                          render={<span tabIndex={0} className="inline-flex shrink-0 text-amber-500" />}
                        >
                          <TriangleAlert className="size-4" />
                        </TooltipTrigger>
                        <TooltipContent>{t('detail.pendingTooltip')}</TooltipContent>
                      </Tooltip>
                    )}
                  </span>
                  <span className="truncate text-xs text-muted-foreground">{entry.url}</span>
                </span>
              </span>
              <span className="flex items-center gap-2 text-muted-foreground">
                <Badge variant="outline">{languageLabel(entry.locale)}</Badge>
                {entry.availableChapters !== null && (
                  <span>
                    {t('detail.chapters')} {entry.availableChapters}
                  </span>
                )}
                <ExternalLinkIcon className="size-4" />
              </span>
            </ExternalLink>
          </li>
        ))}
      </ul>
    );
  }

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
                to={appendFromParam('/login', location.pathname + location.search)}
                className="text-sm text-primary underline-offset-4 hover:underline"
              >
                {t('detail.suggestSource.loginPrompt')}
              </Link>
            ))}
        </div>
        {localeOptions.length > 1 && (
          <ReadingSourceLocaleFilter
            options={localeOptions}
            selectedOptions={selectedLocaleOptions}
            onSelectedOptionsChange={(options) => setSelectedLocaleCodes(options.map((option) => option.value))}
            isAtDefault={isLocaleFilterAtDefault}
            onReset={() => setSelectedLocaleCodes(defaultLocaleCodes)}
          />
        )}
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
        {renderList()}
      </CardContent>
    </Card>
  );
}

export default ComicReadingSources;