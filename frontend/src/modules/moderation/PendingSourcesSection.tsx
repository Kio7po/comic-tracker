import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { approve, findForModerationByStatusIn, reject } from '@/services/source/api/readingSource';
import type { ComicReadingSourceModeration } from '@/services/source/types';
import { ApiError } from '@/common/api/ApiError';
import { ProblemType } from '@/common/api/ProblemType';
import { formatDate } from '@/common/lib/formatDate';
import ConfirmDialog from '@/common/components/ConfirmDialog';
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

  const [sources, setSources] = useState<ComicReadingSourceModeration[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [pendingActionId, setPendingActionId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [selectedSource, setSelectedSource] = useState<ComicReadingSourceModeration | null>(null);
  const [isRejectConfirmOpen, setIsRejectConfirmOpen] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoading(true);
    setHasError(false);
    findForModerationByStatusIn({ statuses: ['PENDING'] }, { signal: controller.signal })
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
    setActionError(null);
  }

  function removeSource(id: number) {
    setSources((previous) => previous.filter((item) => item.source.id !== id));
  }

  function handleActionError(error: unknown, id: number) {
    if (!(error instanceof ApiError)) {
      setActionError(t('moderation.actionError'));
      return;
    }

    // Stale: someone else already reviewed it, or it's gone. Drop it from the list so it stops
    // pretending to still be actionable — the dialog stays open so the user can read why first.
    if (error.type === ProblemType.READING_SOURCE_ALREADY_REVIEWED) {
      removeSource(id);
      setActionError(t('moderation.alreadyReviewedError'));
    } else if (error.type === ProblemType.READING_SOURCE_NOT_FOUND) {
      removeSource(id);
      setActionError(t('moderation.notFoundError'));
    } else {
      setActionError(t('moderation.actionError'));
    }
  }

  async function handleApprove(id: number) {
    setActionError(null);
    setPendingActionId(id);
    try {
      await approve(id);
      removeSource(id);
      closeDialog();
    } catch (error) {
      handleActionError(error, id);
    } finally {
      setPendingActionId(null);
    }
  }

  async function handleReject(id: number) {
    setIsRejectConfirmOpen(false);
    setActionError(null);
    setPendingActionId(id);
    try {
      await reject(id);
      removeSource(id);
      closeDialog();
    } catch (error) {
      handleActionError(error, id);
    } finally {
      setPendingActionId(null);
    }
  }

  function renderList() {
    if (isLoading) {
      return (
        <div className="flex justify-center py-4">
          <Spinner />
        </div>
      );
    }
    if (hasError) {
      return <p className="text-sm text-muted-foreground">{t('moderation.loadError')}</p>;
    }
    if (sources.length === 0) {
      return <p className="text-sm text-muted-foreground">{t('moderation.empty')}</p>;
    }
    return (
      <ul className="mt-2 flex flex-col gap-2">
        {sources.map((item) => (
          <li key={item.source.id}>
            <button
              type="button"
              onClick={() => setSelectedSource(item)}
              className="flex w-full items-center gap-3 rounded-md border border-border px-3 py-2 text-left text-sm transition-colors hover:bg-muted"
            >
              {item.source.iconUrl && (
                <img src={item.source.iconUrl} alt="" className="size-8 shrink-0 rounded-xs" />
              )}
              <span className="flex min-w-0 flex-col">
                <span className="font-medium text-foreground">{item.source.name}</span>
                <span className="truncate text-muted-foreground">{item.source.url}</span>
              </span>
              <span className="ml-auto shrink-0 text-xs text-muted-foreground">
                {formatDate(item.source.createdAt, i18n.language)}
              </span>
            </button>
          </li>
        ))}
      </ul>
    );
  }

  return (
    <>
      <Card>
        <CardContent>
          <h2 className="mb-4 text-sm font-semibold text-muted-foreground uppercase">
            {t('moderation.pendingSources')}
          </h2>
          {renderList()}
        </CardContent>
      </Card>

      <Dialog open={selectedSource !== null} onOpenChange={(open) => !open && closeDialog()}>
        <DialogContent>
          {selectedSource && (
            <>
              <DialogHeader>
                <DialogTitle className="flex items-center gap-2 text-lg">
                  {selectedSource.source.iconUrl && (
                    <img src={selectedSource.source.iconUrl} alt="" className="size-6 rounded-xs" />
                  )}
                  {selectedSource.source.name}
                </DialogTitle>
              </DialogHeader>
              <dl className="flex flex-col gap-2 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.id')}</dt>
                  <dd className="text-foreground">{selectedSource.source.id}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.slug')}</dt>
                  <dd className="text-foreground">{selectedSource.source.slug}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.url')}</dt>
                  <dd className="min-w-0 break-all text-right">
                    <ExternalLink
                      href={selectedSource.source.url}
                      className="text-primary underline-offset-4 hover:underline"
                    >
                      {selectedSource.source.url}
                    </ExternalLink>
                  </dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.status')}</dt>
                  <dd className="text-foreground">{selectedSource.source.status}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.submittedAt')}</dt>
                  <dd className="text-foreground">{formatDate(selectedSource.source.createdAt, i18n.language)}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">{t('moderation.fields.contributedBy')}</dt>
                  <dd className="text-foreground">{selectedSource.contributedBy.username}</dd>
                </div>
              </dl>
              <Separator/>
              <div className="min-h-4">
                {actionError && <p className="text-sm text-destructive">{actionError}</p>}
              </div>
              <DialogFooter>
                <Button
                  disabled={pendingActionId === selectedSource.source.id}
                  onClick={() => handleApprove(selectedSource.source.id)}
                >
                  {t('moderation.approve')}
                </Button>
                <Button
                  variant="destructive"
                  disabled={pendingActionId === selectedSource.source.id}
                  onClick={() => setIsRejectConfirmOpen(true)}
                >
                  {t('moderation.reject')}
                </Button>
              </DialogFooter>
            </>
          )}
        </DialogContent>
      </Dialog>
      <ConfirmDialog
        open={isRejectConfirmOpen}
        onOpenChange={setIsRejectConfirmOpen}
        title={t('moderation.rejectSourceConfirmTitle')}
        description={t('moderation.rejectSourceConfirmDescription')}
        confirmLabel={t('moderation.reject')}
        isConfirming={selectedSource !== null && pendingActionId === selectedSource.source.id}
        onConfirm={() => selectedSource && handleReject(selectedSource.source.id)}
      />
    </>
  );
}

export default PendingSourcesSection;