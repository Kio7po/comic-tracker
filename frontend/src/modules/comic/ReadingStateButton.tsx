import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useLocation, useNavigate } from 'react-router';
import { BookmarkCheck, BookmarkPlus } from 'lucide-react';
import { create, getByComic } from '@/services/readingState/api/readingState';
import type { ReadingState } from '@/services/readingState/types';
import type { ComicSummary } from '@/services/comic/types';
import { useAuth } from '@/common/components/AuthProvider';
import { appendFromParam } from '@/common/lib/authRedirect';
import { ApiError, ProblemType } from '@/common/api';
import { Button } from '@/common/components/ui/button';
import { Spinner } from '@/common/components/ui/spinner';
import { toast } from '@/common/components/ui/toast';
import EditReadingStateDialog from './EditReadingStateDialog';

// Not a backend-mirrored DTO (ComicSummary itself stays untouched, shared as-is with the
// moderation listing, which has no use for chapters) - just this UI slice's own composed shape.
export type ComicWithChapters = ComicSummary & { chapters: number | null };

interface ReadingStateButtonProps {
  comic: ComicWithChapters;
}

function ReadingStateButton({ comic }: Readonly<ReadingStateButtonProps>) {
  const { t } = useTranslation();
  const { user, isLoading: isAuthLoading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const comicSlug = comic.slug;

  const [readingState, setReadingState] = useState<ReadingState | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);

  useEffect(() => {
    if (isAuthLoading) {
      return;
    }
    if (!user) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setIsLoading(false);
      return;
    }

    const controller = new AbortController();
    setIsLoading(true);
    getByComic(comicSlug, { signal: controller.signal })
      .then(setReadingState)
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return;
        }
        if (error instanceof ApiError && error.status === 404) {
          setReadingState(null);
          return;
        }
        // Falls back to the same "not tracked" rendering rather than hiding the button, so the
        // user still has something to act on (retry via the Add button) instead of it silently
        // disappearing.
        toast.add({ title: t('errors.actionFailed'), type: 'error' });
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      });

    return () => {
      controller.abort();
    };
  }, [comicSlug, user, isAuthLoading, t]);

  async function handleAdd() {
    if (!user) {
      navigate(appendFromParam('/login', location.pathname + location.search));
      return;
    }

    setIsSaving(true);
    try {
      setReadingState(
        await create(comicSlug, { status: 'PLAN_TO_READ', chapters: 0, notes: undefined, preferredEntryId: undefined }),
      );
    } catch (error) {
      if (error instanceof ApiError && error.type === ProblemType.READING_STATE_ALREADY_EXISTS) {
        // Race condition (double-click, another tab...): already tracked, not a real failure.
        toast.add({ title: t('detail.alreadyInLibrary'), type: 'info' });
      } else {
        toast.add({ title: t('errors.actionFailed'), type: 'error' });
      }
    } finally {
      setIsSaving(false);
    }
  }

  if (isAuthLoading || (user && isLoading)) {
    return (
      <Button type="button" className="mt-4 w-full" disabled>
        <Spinner />
      </Button>
    );
  }

  if (readingState) {
    return (
      <>
        <Button type="button" variant="outline" className="mt-4 w-full" onClick={() => setIsEditDialogOpen(true)}>
          <BookmarkCheck className="size-4" />
          {t('detail.editLibrary')}
        </Button>
        <EditReadingStateDialog
          comic={comic}
          readingState={readingState}
          open={isEditDialogOpen}
          onOpenChange={setIsEditDialogOpen}
          onUpdated={setReadingState}
          onRemoved={() => setReadingState(null)}
        />
      </>
    );
  }

  return (
    <Button type="button" className="mt-4 w-full" onClick={handleAdd} disabled={isSaving}>
      {isSaving ? <Spinner /> : <BookmarkPlus className="size-4" />}
      {isSaving ? t('detail.addingToLibrary') : t('detail.addToLibrary')}
    </Button>
  );
}

export default ReadingStateButton;
