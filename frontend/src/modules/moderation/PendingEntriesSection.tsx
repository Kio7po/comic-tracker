import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { approve, findByStatusIn, reject } from '@/services/source/api/readingEntry';
import type { ComicReadingEntryModeration } from '@/services/source/types';
import { ApiError } from '@/common/api/ApiError';
import { ProblemType } from '@/common/api/ProblemType';
import { formatDate } from '@/common/lib/formatDate';
import ExternalLink from '@/common/components/ExternalLink';
import { Button } from '@/common/components/ui/button';
import { Card, CardContent } from '@/common/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/common/components/ui/dialog';
import { Spinner } from '@/common/components/ui/spinner';
import { Separator } from '@/common/components/ui/separator';
import SearchPagination from '@/modules/catalog/SearchPagination';

const PAGE_SIZE = 20;

function PendingEntriesSection() {
  const { t, i18n } = useTranslation();

  const [items, setItems] = useState<ComicReadingEntryModeration[]>([]);
  const [totalItems, setTotalItems] = useState<number | null>(null);
  const [existMoreItems, setExistMoreItems] = useState(true);
  const [page, setPage] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [pendingActionId, setPendingActionId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [selectedEntry, setSelectedEntry] = useState<ComicReadingEntryModeration | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoading(true);
    setHasError(false);
    findByStatusIn(
      { statuses: ['PENDING'], limit: PAGE_SIZE, offset: (page - 1) * PAGE_SIZE },
      { signal: controller.signal },
    )
      .then((response) => {
        setItems(response.items);
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
  }, [page]);

  function closeDialog() {
    setSelectedEntry(null);
  }

  function removeItem(id: number) {
    setItems((previous) => previous.filter((item) => item.entry.id !== id));
    setTotalItems((previous) => (previous !== null ? previous - 1 : previous));
  }

  async function handleApprove(id: number) {
    setActionError(null);
    setPendingActionId(id);
    try {
      await approve(id);
      removeItem(id);
      closeDialog();
    } catch (error) {
      if (error instanceof ApiError && error.type === ProblemType.READING_SOURCE_NOT_APPROVED) {
        setActionError(t('moderation.sourceNotApprovedError'));
      } else {
        setActionError(t('moderation.actionError'));
      }
    } finally {
      setPendingActionId(null);
    }
  }

  async function handleReject(id: number) {
    setActionError(null);
    setPendingActionId(id);
    try {
      await reject(id);
      removeItem(id);
      closeDialog();
    } catch {
      setActionError(t('moderation.actionError'));
    } finally {
      setPendingActionId(null);
    }
  }

  const totalPages = totalItems !== null ? Math.max(1, Math.ceil(totalItems / PAGE_SIZE)) : null;

  return (
    <>
      <Card>
        <CardContent>
          <h2 className="mb-4 text-sm font-semibold text-muted-foreground uppercase">
            {t('moderation.pendingEntries')}
          </h2>
          {isLoading ? (
            <div className="flex justify-center py-4">
              <Spinner />
            </div>
          ) : hasError ? (
            <p className="text-sm text-muted-foreground">{t('moderation.loadError')}</p>
          ) : items.length === 0 ? (
            <p className="text-sm text-muted-foreground">{t('moderation.empty')}</p>
          ) : (
            <>
              <ul className="mt-2 flex flex-col gap-2">
                {items.map(({ entry, comic }) => (
                  <li key={entry.id}>
                    <button
                      type="button"
                      onClick={() => setSelectedEntry({ entry, comic })}
                      className="flex w-full items-center gap-3 rounded-md border border-border px-3 py-2 text-left text-sm transition-colors hover:bg-muted"
                    >
                      {comic.coverUrl && (
                        <img src={comic.coverUrl} alt="" className="h-10 w-8 shrink-0 rounded-xs object-cover" />
                      )}
                      <span className="flex min-w-0 flex-col">
                        <span className="font-medium text-foreground">{comic.title}</span>
                        <span className="truncate text-muted-foreground">
                          {entry.source.name} · {entry.url}
                        </span>
                      </span>
                      <span className="ml-auto shrink-0 text-xs text-muted-foreground">
                        {formatDate(entry.createdAt, i18n.language)}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
              <SearchPagination
                page={page}
                onPageChange={setPage}
                totalPages={totalPages}
                existMoreItems={existMoreItems}
                className="justify-center"
              />
            </>
          )}
          {actionError && <p className="mt-3 text-sm text-destructive">{actionError}</p>}
        </CardContent>
      </Card>

      <Dialog open={selectedEntry !== null} onOpenChange={(open) => !open && closeDialog()}>
        <DialogContent>
          {selectedEntry && (
            <>
              <DialogHeader>
                <DialogTitle className="flex items-center gap-2 text-lg">
                  {selectedEntry.comic.coverUrl && (
                    <img src={selectedEntry.comic.coverUrl} alt="" className="h-8 w-6 rounded-xs object-cover" />
                  )}
                  {selectedEntry.comic.title}
                </DialogTitle>
              </DialogHeader>
              <dl className="flex flex-col gap-2 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.id')}</dt>
                  <dd className="text-foreground">{selectedEntry.entry.id}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.comic')}</dt>
                  <dd className="text-foreground">
                    <Link
                      to={`/comics/${selectedEntry.comic.slug}`}
                      className="underline-offset-2 hover:underline"
                    >
                      {selectedEntry.comic.title}
                    </Link>
                  </dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.source')}</dt>
                  <dd className="text-foreground">{selectedEntry.entry.source.name}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.url')}</dt>
                  <dd className="min-w-0 truncate text-right">
                    <ExternalLink
                      href={selectedEntry.entry.url}
                      className="text-foreground underline-offset-2 hover:underline"
                    >
                      {selectedEntry.entry.url}
                    </ExternalLink>
                  </dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.locale')}</dt>
                  <dd className="text-foreground">{selectedEntry.entry.locale}</dd>
                </div>
                {selectedEntry.entry.availableChapters !== null && (
                  <div className="flex justify-between gap-4">
                    <dt className="text-muted-foreground">{t('moderation.fields.availableChapters')}</dt>
                    <dd className="text-foreground">{selectedEntry.entry.availableChapters}</dd>
                  </div>
                )}
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.status')}</dt>
                  <dd className="text-foreground">{selectedEntry.entry.status}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.submittedAt')}</dt>
                  <dd className="text-foreground">{formatDate(selectedEntry.entry.createdAt, i18n.language)}</dd>
                </div>
              </dl>
              <Separator/>
              <DialogFooter>
                <Button
                  disabled={pendingActionId === selectedEntry.entry.id}
                  onClick={() => handleApprove(selectedEntry.entry.id)}
                >
                  {t('moderation.approve')}
                </Button>
                <Button
                  variant="destructive"
                  disabled={pendingActionId === selectedEntry.entry.id}
                  onClick={() => handleReject(selectedEntry.entry.id)}
                >
                  {t('moderation.reject')}
                </Button>
              </DialogFooter>
            </>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}

export default PendingEntriesSection;