import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { BookmarkOff, Info, MoreHorizontal, Save, Minus, Share2, SquarePen, Plus } from 'lucide-react';
import type { ReadingStateStatus } from '@/services/readingState/types';
import { cn } from '@/common/lib/utils';
import { Badge } from '@/common/components/ui/badge';
import { Button } from '@/common/components/ui/button';
import { Card } from '@/common/components/ui/card';
import ConfirmDialog from '@/common/components/ConfirmDialog';
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
import type { LibraryEntry } from './types';

// Badge's built-in variants don't cover this - each status gets its own pastel color instead.
// PLAN_TO_READ copies the plain `secondary` variant's own classes (already a neutral pastel).
const STATUS_BADGE_CLASSNAME: Record<ReadingStateStatus, string> = {
  PLAN_TO_READ: 'bg-secondary text-secondary-foreground',
  READING: 'bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-300',
  COMPLETED: 'bg-green-100 text-green-700 dark:bg-green-500/15 dark:text-green-300',
  ON_HOLD: 'bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300',
  DROPPED: 'bg-red-100 text-red-700 dark:bg-red-500/15 dark:text-red-300',
};

function LibraryComicCard({ entry }: Readonly<{ entry: LibraryEntry }>) {
  const { t } = useTranslation();
  const { comic, readingState } = entry;
  const { status, chapters } = readingState;
  const pendingChapters = comic.chapters !== null ? comic.chapters - chapters : 0;
  const [isRemoveConfirmOpen, setIsRemoveConfirmOpen] = useState(false);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);

  async function handleShare() {
    const url = `${window.location.origin}/comics/${comic.slug}`;
    try {
      await navigator.clipboard.writeText(url);
      toast.add({ title: t('library.actions.shareSuccess'), type: 'success' });
    } catch {
      toast.add({ title: t('errors.actionFailed'), type: 'error' });
    }
  }

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
                  <Button type="button" variant="ghost" size="icon" className="rounded-none">
                    <Minus className="size-5" />
                  </Button>
                  <span className="min-w-10 px-1 text-center text-base font-medium text-foreground">{chapters}</span>
                  <Button type="button" variant="ghost" size="icon" className="rounded-none">
                    <Plus className="size-5" />
                  </Button>
                </div>
                <Button type="button" variant="default" size="icon" aria-label={t('library.actions.save')}>
                  <Save className="size-5" />
                </Button>
              </div>
            </div>
            <DrawerFooter>
              <Button type="button" size="lg" variant="outline" render={<Link to={`/comics/${comic.slug}`} />}>
                <Info className="size-5" />
                {t('library.actions.viewDetails')}
              </Button>
              <Button type="button" size="lg" variant="outline">
                <SquarePen className="size-5" />
                {t('detail.editLibrary')}
              </Button>
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
          className={`absolute top-2 right-2 flex size-4 items-center justify-center rounded-full p-0 text-[10px] leading-none sm:h-5 sm:w-fit sm:rounded-4xl sm:px-2 sm:py-0.5 sm:text-xs ${STATUS_BADGE_CLASSNAME[status]}`}
        >
          <span className="sm:hidden">{t(`detail.readingState.statuses.${status}`).charAt(0).toUpperCase()}</span>
          <span className="hidden sm:inline">{t(`detail.readingState.statuses.${status}`)}</span>
        </Badge>
        {/* pointer-events-none on the full-card darken layer so hovering/clicking the empty area
            still reaches the Link underneath; the bottom rectangle opts back in since it's a
            real toolbar. Not functional yet - a quick way to bump the read-chapter count
            without opening the full dialog. Hover-capable devices only - touch gets the Drawer
            trigger below instead. */}
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
              <Button type="button" variant="outline" size="icon-sm">
                <SquarePen className="size-5" />
              </Button>
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
                <Button type="button" variant="ghost" size="icon-sm" className="rounded-none">
                  <Minus className="size-4" />
                </Button>
                <span className="min-w-6 px-1 text-center text-sm font-medium text-popover-foreground">
                  {chapters}
                </span>
                <Button type="button" variant="ghost" size="icon-sm" className="rounded-none">
                  <Plus className="size-4" />
                </Button>
              </div>
              <Button type="button" variant="default" size="icon-sm">
                <Save className="size-5" />
              </Button>
            </div>
          </div>
        </div>
      </Card>
      <p className="mt-2 truncate text-sm font-medium text-foreground">{comic.title}</p>
      <ConfirmDialog
        open={isRemoveConfirmOpen}
        onOpenChange={setIsRemoveConfirmOpen}
        title={t('detail.readingState.removeConfirmTitle')}
        description={t('detail.readingState.removeConfirmDescription')}
        confirmLabel={t('library.actions.remove')}
        onConfirm={() => setIsRemoveConfirmOpen(false)}
      />
    </div>
  );
}

export default LibraryComicCard;
