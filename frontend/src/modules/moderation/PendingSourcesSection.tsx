import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { approve, findByStatusIn, reject } from '@/services/source/api/readingSource';
import type { ComicReadingSource } from '@/services/source/types';
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

function PendingSourcesSection() {
  const { t, i18n } = useTranslation();

  const [sources, setSources] = useState<ComicReadingSource[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [pendingActionId, setPendingActionId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [selectedSource, setSelectedSource] = useState<ComicReadingSource | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoading(true);
    setHasError(false);
    findByStatusIn({ statuses: ['PENDING'] }, { signal: controller.signal })
      .then(setSources)
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

  function closeDialog() {
    setSelectedSource(null);
  }

  async function handleApprove(id: number) {
    setActionError(null);
    setPendingActionId(id);
    try {
      await approve(id);
      setSources((previous) => previous.filter((source) => source.id !== id));
      closeDialog();
    } catch {
      setActionError(t('moderation.actionError'));
    } finally {
      setPendingActionId(null);
    }
  }

  async function handleReject(id: number) {
    setActionError(null);
    setPendingActionId(id);
    try {
      await reject(id);
      setSources((previous) => previous.filter((source) => source.id !== id));
      closeDialog();
    } catch {
      setActionError(t('moderation.actionError'));
    } finally {
      setPendingActionId(null);
    }
  }

  return (
    <>
      <Card>
        <CardContent>
          <h2 className="mb-4 text-sm font-semibold text-muted-foreground uppercase">
            {t('moderation.pendingSources')}
          </h2>
          {isLoading ? (
            <div className="flex justify-center py-4">
              <Spinner />
            </div>
          ) : hasError ? (
            <p className="text-sm text-muted-foreground">{t('moderation.loadError')}</p>
          ) : sources.length === 0 ? (
            <p className="text-sm text-muted-foreground">{t('moderation.empty')}</p>
          ) : (
            <ul className="mt-2 flex flex-col gap-2">
              {sources.map((source) => (
                <li key={source.id}>
                  <button
                    type="button"
                    onClick={() => setSelectedSource(source)}
                    className="flex w-full items-center gap-3 rounded-md border border-border px-3 py-2 text-left text-sm transition-colors hover:bg-muted"
                  >
                    {source.iconUrl && (
                      <img src={source.iconUrl} alt="" className="size-8 shrink-0 rounded-xs" />
                    )}
                    <span className="flex min-w-0 flex-col">
                      <span className="font-medium text-foreground">{source.name}</span>
                      <span className="truncate text-muted-foreground">{source.url}</span>
                    </span>
                    <span className="ml-auto shrink-0 text-xs text-muted-foreground">
                      {formatDate(source.createdAt, i18n.language)}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
          {actionError && <p className="mt-3 text-sm text-destructive">{actionError}</p>}
        </CardContent>
      </Card>

      <Dialog open={selectedSource !== null} onOpenChange={(open) => !open && closeDialog()}>
        <DialogContent>
          {selectedSource && (
            <>
              <DialogHeader>
                <DialogTitle className="flex items-center gap-2 text-lg">
                  {selectedSource.iconUrl && (
                    <img src={selectedSource.iconUrl} alt="" className="size-6 rounded-xs" />
                  )}
                  {selectedSource.name}
                </DialogTitle>
              </DialogHeader>
              <dl className="flex flex-col gap-2 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.id')}</dt>
                  <dd className="text-foreground">{selectedSource.id}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.slug')}</dt>
                  <dd className="text-foreground">{selectedSource.slug}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.url')}</dt>
                  <dd className="min-w-0 truncate text-right">
                    <ExternalLink
                      href={selectedSource.url}
                      className="text-foreground underline-offset-2 hover:underline"
                    >
                      {selectedSource.url}
                    </ExternalLink>
                  </dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.status')}</dt>
                  <dd className="text-foreground">{selectedSource.status}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.submittedAt')}</dt>
                  <dd className="text-foreground">{formatDate(selectedSource.createdAt, i18n.language)}</dd>
                </div>
              </dl>
              <Separator/>
              <DialogFooter>
                <Button
                  disabled={pendingActionId === selectedSource.id}
                  onClick={() => handleApprove(selectedSource.id)}
                >
                  {t('moderation.approve')}
                </Button>
                <Button
                  variant="destructive"
                  disabled={pendingActionId === selectedSource.id}
                  onClick={() => handleReject(selectedSource.id)}
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

export default PendingSourcesSection;