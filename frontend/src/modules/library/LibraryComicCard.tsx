import { memo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import {
  BookmarkOff,
  ExternalLink as ExternalLinkIcon,
  Info,
  MoreHorizontal,
  Save,
  Minus,
  Share2,
  SquarePen,
  Plus,
} from 'lucide-react';
import { remove, update } from '@/services/readingState/api/readingState';
import type { ReadingState, ReadingStateStatus, ReadingStateWithComic } from '@/services/readingState/types';
import { cn } from '@/common/lib/utils';
import { Badge } from '@/common/components/ui/badge';
import { Button } from '@/common/components/ui/button';
import { Card } from '@/common/components/ui/card';
import { Spinner } from '@/common/components/ui/spinner';
import ConfirmDialog from '@/common/components/ConfirmDialog';
import ExternalLink from '@/common/components/ExternalLink';
import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
  DrawerTrigger,
} from '@/common/components/ui/drawer';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/common/components/ui/dropdown-menu';
import { toast } from '@/common/components/ui/toast';
import TruncatedText from '@/common/components/TruncatedText';
import EditReadingStateDialog from '@/modules/comic/EditReadingStateDialog';

// Badge's built-in variants don't cover this - each status gets its own pastel color instead.
// No translucent overlay (e.g. blue-500/15), it'd lose contrast.
const STATUS_BADGE_CLASSNAME: Record<ReadingStateStatus, string> = {
  PLAN_TO_READ: 'bg-secondary text-secondary-foreground',
  READING: 'bg-blue-100 text-blue-700 dark:bg-blue-950 dark:text-blue-300',
  COMPLETED: 'bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-300',
  ON_HOLD: 'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300',
  DROPPED: 'bg-red-100 text-red-700 dark:bg-red-950 dark:text-red-300',
};

interface LibraryComicCardProps {
  entry: ReadingStateWithComic;
  onUpdated: (comicSlug: string, readingState: ReadingState) => void;
  onRemoved: (comicSlug: string) => void;
}

function LibraryComicCard({ entry, onUpdated, onRemoved }: Readonly<LibraryComicCardProps>) {
  const { t } = useTranslation();
  const { comic, readingState } = entry;
  const { status, chapters } = readingState;
  const pendingChapters = comic.chapters !== null ? comic.chapters - chapters : 0;
  const [isRemoveConfirmOpen, setIsRemoveConfirmOpen] = useState(false);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isRemoving, setIsRemoving] = useState(false);
  const [isSavingChapters, setIsSavingChapters] = useState(false);

  // Render-time sync (not an effect): draftChapters tracks the stepper's in-progress edit, reset
  // whenever the underlying readingState.chapters actually changes (a real save landed).
  const [previousChapters, setPreviousChapters] = useState(chapters);
  const [draftChapters, setDraftChapters] = useState(chapters);
  if (chapters !== previousChapters) {
    setPreviousChapters(chapters);
    setDraftChapters(chapters);
  }

  async function handleShare() {
    const url = `${window.location.origin}/comics/${comic.slug}`;

    if (navigator.share) {
      try {
        await navigator.share({ title: comic.title, url });
      } catch (error) {
        // AbortError: the user dismissed the native share sheet - not a real failure.
        if (error instanceof DOMException && error.name === 'AbortError') return;
        toast.add({ title: t('errors.actionFailed'), type: 'error' });
      }
      return;
    }

    try {
      await navigator.clipboard.writeText(url);
      toast.add({ title: t('library.actions.shareSuccess'), type: 'success' });
    } catch {
      toast.add({ title: t('errors.actionFailed'), type: 'error' });
    }
  }

  async function handleSaveChapters() {
    setIsSavingChapters(true);
    try {
      onUpdated(
        comic.slug,
        await update(comic.slug, {
          status: readingState.status,
          chapters: draftChapters,
          notes: readingState.notes ?? undefined,
          preferredEntryId: readingState.preferredEntry?.id ?? undefined,
        }),
      );
    } catch {
      toast.add({ title: t('errors.actionFailed'), type: 'error' });
    } finally {
      setIsSavingChapters(false);
    }
  }

  async function handleConfirmRemove() {
    setIsRemoving(true);
    try {
      await remove(comic.slug);
      onRemoved(comic.slug);
    } catch {
      toast.add({ title: t('errors.actionFailed'), type: 'error' });
    } finally {
      setIsRemoving(false);
      setIsRemoveConfirmOpen(false);
    }
  }

  const hasChapterChanges = draftChapters !== chapters;

  return (
    <div>
      <Card size="sm" className="group relative py-0">
        <Link to={`/comics/${comic.slug}`} className="hidden sm:block">
          {comic.coverUrl ? (
            <img src={comic.coverUrl} alt="" className="aspect-2/3 w-full object-cover" />
          ) : (
            <div className="aspect-2/3 w-full bg-muted" />
          )}
        </Link>
        <Drawer>
          <DrawerTrigger
            className="block w-full sm:hidden"
            render={<button type="button" aria-label={t('library.actions.trigger')} />}
          >
            {comic.coverUrl ? (
              <img src={comic.coverUrl} alt="" className="aspect-2/3 w-full object-cover" />
            ) : (
              <div className="aspect-2/3 w-full bg-muted" />
            )}
          </DrawerTrigger>
          <DrawerContent>
            <DrawerHeader>
              <DrawerTitle className="truncate">{comic.title}</DrawerTitle>
            </DrawerHeader>
            <div className="flex items-center justify-between gap-3 px-4 py-4">
              <span className="text-base font-medium text-foreground">{t('detail.readingState.chaptersRead')}</span>
              <div className="flex items-center gap-3">
                <div className="flex items-center overflow-hidden rounded-md border border-input">
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="rounded-none"
                    disabled={draftChapters <= 0}
                    onClick={() => setDraftChapters((value) => Math.max(0, value - 1))}
                  >
                    <Minus className="size-5" />
                  </Button>
                  <span className="min-w-10 px-1 text-center text-base font-medium text-foreground">
                    {draftChapters}
                  </span>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="rounded-none"
                    onClick={() => setDraftChapters((value) => value + 1)}
                  >
                    <Plus className="size-5" />
                  </Button>
                </div>
                <Button
                  type="button"
                  variant="default"
                  size="icon"
                  aria-label={t('library.actions.save')}
                  disabled={!hasChapterChanges || isSavingChapters}
                  onClick={handleSaveChapters}
                >
                  {isSavingChapters ? <Spinner /> : <Save className="size-5" />}
                </Button>
              </div>
            </div>
            <DrawerFooter>
              <Button type="button" size="lg" variant="outline" render={<Link to={`/comics/${comic.slug}`} />}>
                <Info className="size-5" />
                {t('library.actions.viewDetails')}
              </Button>
              <DrawerClose
                render={<Button type="button" size="lg" variant="outline" onClick={() => setIsEditDialogOpen(true)} />}
              >
                <SquarePen className="size-5" />
                {t('detail.editLibrary')}
              </DrawerClose>
              {readingState.preferredEntry && (
                <Button
                  type="button"
                  size="lg"
                  variant="outline"
                  render={<ExternalLink href={readingState.preferredEntry.url} />}
                >
                  <ExternalLinkIcon className="size-5" />
                  {t('library.actions.continueReading')}
                </Button>
              )}
              <Button type="button" size="lg" variant="outline" onClick={handleShare}>
                <Share2 className="size-5" />
                {t('library.actions.share')}
              </Button>
              <Button type="button" size="lg" variant="destructive" onClick={() => setIsRemoveConfirmOpen(true)}>
                <BookmarkOff className="size-5" />
                {t('library.actions.remove')}
              </Button>
              <DrawerClose render={<Button type="button" size="lg" variant="ghost" />}>
                {t('common.cancel')}
              </DrawerClose>
            </DrawerFooter>
          </DrawerContent>
        </Drawer>
        {pendingChapters > 0 && <Badge className="absolute top-2 left-2">{`+${pendingChapters}`}</Badge>}
        <Badge
          className={`absolute top-2 right-2 flex size-5 items-center justify-center rounded-full leading-none sm:h-5 sm:w-fit sm:rounded-4xl sm:px-2 sm:py-0.5 sm:text-xs ${STATUS_BADGE_CLASSNAME[status]}`}
        >
          <span className="sm:hidden">{t(`detail.readingState.statuses.${status}`).charAt(0).toUpperCase()}</span>
          <span className="hidden sm:inline">{t(`detail.readingState.statuses.${status}`)}</span>
        </Badge>
        {/* pointer-events-none on the full-card darken layer so hovering/clicking the empty area
            still reaches the Link underneath; the bottom rectangle opts back in since it's a
            real toolbar. Hover-capable devices only - touch gets the Drawer trigger below
            instead. */}
        <div
          className={cn(
            'pointer-events-none absolute inset-0 hidden bg-black/50 opacity-0 transition-opacity group-hover:opacity-100 sm:block',
            isDropdownOpen && 'opacity-100',
          )}
        >
          <div
            className={cn(
              'pointer-events-none absolute inset-x-2 bottom-2 flex flex-col items-start gap-1.5 group-hover:pointer-events-auto',
              isDropdownOpen && 'pointer-events-auto',
            )}
          >
            <div className="flex items-center p-1.5 gap-2 rounded-md bg-popover">
              <Button type="button" variant="outline" size="icon-sm" onClick={() => setIsEditDialogOpen(true)}>
                <SquarePen className="size-5" />
              </Button>
              {readingState.preferredEntry && (
                <Button
                  type="button"
                  variant="outline"
                  size="icon-sm"
                  aria-label={t('library.actions.continueReading')}
                  title={t('library.actions.continueReading')}
                  render={<ExternalLink href={readingState.preferredEntry.url} />}
                >
                  <ExternalLinkIcon className="size-5" />
                </Button>
              )}
              <DropdownMenu onOpenChange={setIsDropdownOpen}>
                <DropdownMenuTrigger
                  render={
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon-sm"
                      aria-label={t('library.actions.trigger')}
                    />
                  }
                >
                  <MoreHorizontal className="size-5" />
                </DropdownMenuTrigger>
                <DropdownMenuContent>
                  <DropdownMenuItem onClick={handleShare}>
                    <Share2 className="size-4" />
                    {t('library.actions.share')}
                  </DropdownMenuItem>
                  <DropdownMenuItem variant="destructive" onClick={() => setIsRemoveConfirmOpen(true)}>
                    <BookmarkOff className="size-4" />
                    {t('library.actions.remove')}
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
            <div className="flex items-center justify-center gap-2 self-stretch rounded-md bg-popover py-1.5">
              <div className="flex items-center overflow-hidden rounded-md border border-input">
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-sm"
                  className="rounded-none"
                  disabled={draftChapters <= 0}
                  onClick={() => setDraftChapters((value) => Math.max(0, value - 1))}
                >
                  <Minus className="size-4" />
                </Button>
                <span className="min-w-6 px-1 text-center text-sm font-medium text-popover-foreground">
                  {draftChapters}
                </span>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-sm"
                  className="rounded-none"
                  onClick={() => setDraftChapters((value) => value + 1)}
                >
                  <Plus className="size-4" />
                </Button>
              </div>
              <Button
                type="button"
                variant="default"
                size="icon-sm"
                aria-label={t('library.actions.save')}
                disabled={!hasChapterChanges || isSavingChapters}
                onClick={handleSaveChapters}
              >
                {isSavingChapters ? <Spinner /> : <Save className="size-5" />}
              </Button>
            </div>
          </div>
        </div>
      </Card>
      <TruncatedText text={comic.title} className="mt-2 text-sm font-medium text-foreground" />
      <ConfirmDialog
        open={isRemoveConfirmOpen}
        onOpenChange={setIsRemoveConfirmOpen}
        title={t('detail.readingState.removeConfirmTitle')}
        description={t('detail.readingState.removeConfirmDescription')}
        confirmLabel={t('library.actions.remove')}
        isConfirming={isRemoving}
        onConfirm={handleConfirmRemove}
      />
      <EditReadingStateDialog
        comic={comic}
        readingState={readingState}
        open={isEditDialogOpen}
        onOpenChange={setIsEditDialogOpen}
        onUpdated={(newReadingState) => onUpdated(comic.slug, newReadingState)}
        onRemoved={() => onRemoved(comic.slug)}
      />
    </div>
  );
}

export default memo(LibraryComicCard);
