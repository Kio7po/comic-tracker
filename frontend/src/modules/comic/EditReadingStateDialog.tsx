import { useEffect, useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ChevronsLeft, ChevronsRight, Minus, Plus, BookmarkOff, TriangleAlert } from 'lucide-react';
import { remove, update } from '@/services/readingState/api/readingState';
import type { ReadingState, ReadingStateStatus } from '@/services/readingState/types';
import { findByComic } from '@/services/source/api/readingEntry';
import type { ComicReadingEntry } from '@/services/source/types';
import { ApiError, ProblemType } from '@/common/api';
import { MOBILE_QUERY, useMediaQuery } from '@/common/hooks/useMediaQuery';
import ConfirmDialog from '@/common/components/ConfirmDialog';
import { Badge } from '@/common/components/ui/badge';
import { Button } from '@/common/components/ui/button';
import { toast } from '@/common/components/ui/toast';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/common/components/ui/dialog';
import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from '@/common/components/ui/drawer';
import { Field, FieldError, FieldGroup, FieldLabel } from '@/common/components/ui/field';
import { Input } from '@/common/components/ui/input';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/common/components/ui/select';
import { Spinner } from '@/common/components/ui/spinner';
import { Textarea } from '@/common/components/ui/textarea';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/common/components/ui/tooltip';
import type { ComicWithChapters } from './ReadingStateButton';

const STATUSES: ReadingStateStatus[] = ['READING', 'COMPLETED', 'ON_HOLD', 'PLAN_TO_READ', 'DROPPED'];
// Matches @Size(max = 2048) on the backend's ReadingStateRequestDto.notes.
const NOTES_MAX_LENGTH = 2048;
// Entry ids are always numeric, so this can never collide with a real one.
const NO_PREFERRED_ENTRY = 'none';

function languageLabel(locale: string): string {
  return locale.split('-')[0].toUpperCase();
}

interface EditReadingStateDialogProps {
  comic: ComicWithChapters;
  readingState: ReadingState;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUpdated: (readingState: ReadingState) => void;
  onRemoved: () => void;
}

function EditReadingStateDialog({
  comic,
  readingState,
  open,
  onOpenChange,
  onUpdated,
  onRemoved,
}: Readonly<EditReadingStateDialogProps>) {
  const { t } = useTranslation();
  const isMobile = useMediaQuery(MOBILE_QUERY);
  const statusId = useId();
  const chaptersId = useId();
  const preferredEntrySelectId = useId();
  const notesId = useId();

  const [status, setStatus] = useState<ReadingStateStatus>(readingState.status);
  const [chapters, setChapters] = useState(readingState.chapters);
  const [notes, setNotes] = useState(readingState.notes ?? '');
  const [selectedEntryId, setSelectedEntryId] = useState(readingState.preferredEntry?.id ?? null);
  const [entries, setEntries] = useState<ComicReadingEntry[]>([]);
  const [isLoadingEntries, setIsLoadingEntries] = useState(false);
  const [hasEntriesLoadError, setHasEntriesLoadError] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isRemoving, setIsRemoving] = useState(false);
  const [isRemoveConfirmOpen, setIsRemoveConfirmOpen] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  // open is a controlled prop set by the caller's own button, not by this Dialog's internal
  // trigger, so onOpenChange alone never sees an "opened" transition to reset the form from,
  // sync it here instead, the same way a changing prop is synced anywhere else.
  const [previousOpen, setPreviousOpen] = useState(open);
  if (open !== previousOpen) {
    setPreviousOpen(open);
    if (open) {
      setStatus(readingState.status);
      setChapters(readingState.chapters);
      setNotes(readingState.notes ?? '');
      setSelectedEntryId(readingState.preferredEntry?.id ?? null);
      setFormError(null);
    }
  }

  // This dialog is mounted once per card up front (open/closed is just a prop), so the fetch has
  // to be gated on the open transition rather than on mount - otherwise every card in a library
  // listing would fetch its comic's reading entries immediately, defeating the point of loading
  // them lazily.
  useEffect(() => {
    if (!open) return;

    const controller = new AbortController();
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoadingEntries(true);
    setHasEntriesLoadError(false);
    findByComic(comic.slug, undefined, { signal: controller.signal })
      .then((fetchedEntries) => {
        setEntries(fetchedEntries);
        // REJECTED entries are marked as obsolete, not hidden
        setSelectedEntryId((current) =>
          current !== null && !fetchedEntries.some((entry) => entry.id === current) ? null : current,
        );
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return;
        }
        setHasEntriesLoadError(true);
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setIsLoadingEntries(false);
        }
      });

    return () => {
      controller.abort();
    };
  }, [open, comic.slug]);

  async function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);
    setIsSaving(true);
    try {
      const trimmedNotes = notes.trim();
      onUpdated(
        await update(comic.slug, {
          status,
          chapters,
          notes: trimmedNotes === '' ? undefined : trimmedNotes,
          preferredEntryId: selectedEntryId ?? undefined,
        }),
      );
      onOpenChange(false);
    } catch (error) {
      if (error instanceof ApiError && error.type === ProblemType.READING_STATE_NOT_FOUND) {
        // Race condition (removed elsewhere - another tab, the card's own quick actions...):
        // the state this form was editing is already gone. Not a real failure.
        toast.add({ title: t('detail.readingState.errors.alreadyRemoved'), type: 'info' });
        onRemoved();
        onOpenChange(false);
        return;
      }
      setFormError(t('detail.readingState.errors.generic'));
    } finally {
      setIsSaving(false);
    }
  }

  async function handleRemove() {
    setIsRemoveConfirmOpen(false);
    setFormError(null);
    setIsRemoving(true);
    try {
      await remove(comic.slug);
      onRemoved();
      onOpenChange(false);
    } catch (error) {
      if (error instanceof ApiError && error.type === ProblemType.READING_STATE_NOT_FOUND) {
        // Already removed elsewhere - the goal of this action already holds, we treat it the
        // same as a successful removal.
        toast.add({ title: t('detail.readingState.errors.alreadyRemoved'), type: 'info' });
        onRemoved();
        onOpenChange(false);
        return;
      }
      setFormError(t('detail.readingState.errors.generic'));
    } finally {
      setIsRemoving(false);
    }
  }

  const statusOptions = STATUSES.map((value) => ({ value, label: t(`detail.readingState.statuses.${value}`) }));
  // A REJECTED entry is never selectable going forward, but if it's already the saved
  // preference it still needs to show here (marked obsolete) instead of silently vanishing
  const selectableEntries = entries.filter((entry) => entry.status !== 'REJECTED' || entry.id === selectedEntryId);
  const entryOptions = [
    { value: NO_PREFERRED_ENTRY, label: t('detail.readingState.preferredEntryNone') },
    ...selectableEntries.map((entry) => ({ value: String(entry.id), label: entry.source.name })),
  ];
  const hasChanges =
    status !== readingState.status ||
    chapters !== readingState.chapters ||
    notes.trim() !== (readingState.notes ?? '') ||
    selectedEntryId !== (readingState.preferredEntry?.id ?? null);

  // Shared between the trigger's closed-state display and each dropdown item, so both agree on
  // what an entry looks like instead of the trigger falling back to plain text.
  function renderEntryLabel(entry: ComicReadingEntry) {
    return (
      <span className="flex min-w-0 items-center gap-1.5">
        <span className="truncate">{entry.source.name}</span>
        <Badge variant="outline">{languageLabel(entry.locale)}</Badge>
        {entry.status === 'PENDING' && (
          <span className="inline-flex shrink-0 text-amber-500" title={t('detail.pendingTooltip')}>
            <TriangleAlert className="size-3.5" />
          </span>
        )}
        {entry.status === 'REJECTED' && (
          <Badge variant="destructive" title={t('detail.rejectedTooltip')}>
            {t('detail.readingState.obsolete')}
          </Badge>
        )}
      </span>
    );
  }

  function renderFields() {
    return (
      <FieldGroup className="flex-1">
        <Field>
          <FieldLabel htmlFor={statusId}>{t('detail.readingState.status')}</FieldLabel>
          <Select
            value={status}
            items={statusOptions}
            onValueChange={(value) => setStatus(value as ReadingStateStatus)}
          >
            <SelectTrigger id={statusId} className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent alignItemWithTrigger={!isMobile}>
              <SelectGroup>
                {statusOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectGroup>
            </SelectContent>
          </Select>
        </Field>
        <Field>
          <FieldLabel htmlFor={chaptersId}>{t('detail.readingState.chaptersRead')}</FieldLabel>
          <div className="flex items-center gap-2">
            <Tooltip>
              <TooltipTrigger
                render={
                  <Button
                    type="button"
                    variant="outline"
                    size="icon-sm"
                    aria-label={t('detail.readingState.resetChapters')}
                    disabled={chapters === 0}
                    onClick={() => setChapters(0)}
                  />
                }
              >
                <ChevronsLeft className="size-4" />
              </TooltipTrigger>
              <TooltipContent>{t('detail.readingState.resetChapters')}</TooltipContent>
            </Tooltip>
            <Button
              type="button"
              variant="outline"
              size="icon-sm"
              disabled={chapters <= 0}
              onClick={() => setChapters((value) => Math.max(0, value - 1))}
            >
              <Minus className="size-4" />
            </Button>
            <Input
              id={chaptersId}
              type="number"
              min={0}
              value={chapters}
              onChange={(event) => setChapters(Math.max(0, Number(event.target.value) || 0))}
              className="text-center"
            />
            <Button
              type="button"
              variant="outline"
              size="icon-sm"
              onClick={() => setChapters((value) => value + 1)}
            >
              <Plus className="size-4" />
            </Button>
            <Tooltip>
              <TooltipTrigger
                render={
                  <Button
                    type="button"
                    variant="outline"
                    size="icon-sm"
                    aria-label={t('detail.readingState.setToMaxChapters')}
                    disabled={comic.chapters === null}
                    onClick={() => comic.chapters !== null && setChapters(comic.chapters)}
                  />
                }
              >
                <ChevronsRight className="size-4" />
              </TooltipTrigger>
              <TooltipContent>{t('detail.readingState.setToMaxChapters')}</TooltipContent>
            </Tooltip>
          </div>
        </Field>
        <Field>
          <FieldLabel htmlFor={preferredEntrySelectId}>{t('detail.readingState.preferredEntry')}</FieldLabel>
          <Select
            value={selectedEntryId === null ? NO_PREFERRED_ENTRY : String(selectedEntryId)}
            items={entryOptions}
            disabled={isLoadingEntries || hasEntriesLoadError}
            onValueChange={(value) => setSelectedEntryId(value === NO_PREFERRED_ENTRY ? null : Number(value))}
          >
            <SelectTrigger id={preferredEntrySelectId} className="w-full">
              <SelectValue>
                {(value: string) => {
                  if (isLoadingEntries) return t('detail.readingState.loadingEntries');
                  if (hasEntriesLoadError) return t('detail.readingState.entriesLoadError');
                  if (value === NO_PREFERRED_ENTRY) return t('detail.readingState.preferredEntryNone');
                  const entry = entries.find((candidate) => String(candidate.id) === value);
                  return entry ? renderEntryLabel(entry) : t('detail.readingState.preferredEntryNone');
                }}
              </SelectValue>
            </SelectTrigger>
            <SelectContent alignItemWithTrigger={!isMobile}>
              <SelectGroup>
                <SelectItem value={NO_PREFERRED_ENTRY}>{t('detail.readingState.preferredEntryNone')}</SelectItem>
                {selectableEntries.map((entry) => (
                  <SelectItem key={entry.id} value={String(entry.id)} disabled={entry.status === 'REJECTED'}>
                    {renderEntryLabel(entry)}
                  </SelectItem>
                ))}
              </SelectGroup>
            </SelectContent>
          </Select>
        </Field>
        <Field>
          <FieldLabel htmlFor={notesId}>{t('detail.readingState.notes')}</FieldLabel>
          <Textarea
            id={notesId}
            placeholder={t('detail.readingState.notesPlaceholder')}
            value={notes}
            maxLength={NOTES_MAX_LENGTH}
            onChange={(event) => setNotes(event.target.value)}
          />
        </Field>
        <div className="min-h-4 mb-3">
          <FieldError className="text-xs sm:text-right">{formError}</FieldError>
        </div>
      </FieldGroup>
    );
  }

  function renderRemoveButton() {
    return (
      <Button
        type="button"
        variant="destructive"
        size={isMobile ? 'lg' : 'default'}
        disabled={isRemoving}
        onClick={() => setIsRemoveConfirmOpen(true)}
      >
        {isRemoving ? <Spinner /> : <BookmarkOff className="size-4" />}
        {isRemoving ? t('detail.readingState.removing') : t('detail.readingState.remove')}
      </Button>
    );
  }

  function renderUpdateButton() {
    return (
      <Button type="submit" size={isMobile ? 'lg' : 'default'} disabled={isSaving || !hasChanges}>
        {isSaving && <Spinner />}
        {isSaving ? t('detail.readingState.updating') : t('detail.readingState.update')}
      </Button>
    );
  }

  const confirmRemoveDialog = (
    <ConfirmDialog
      open={isRemoveConfirmOpen}
      onOpenChange={setIsRemoveConfirmOpen}
      title={t('detail.readingState.removeConfirmTitle')}
      description={t('detail.readingState.removeConfirmDescription')}
      confirmLabel={t('detail.readingState.remove')}
      isConfirming={isRemoving}
      onConfirm={handleRemove}
    />
  );

  // Select nested inside Dialog has a real upstream focus/positioning bug on mobile (the dialog
  // scrolls the page and briefly resizes when the select opens) - using a Drawer there sidesteps
  // it entirely instead of working around it. Desktop keeps the Dialog.
  if (isMobile) {
    return (
      <>
        <Drawer open={open} onOpenChange={onOpenChange}>
          <DrawerContent>
            <div className="relative shrink-0 overflow-hidden rounded-t-xl">
              {comic.coverUrl && (
                <img
                  src={comic.coverUrl}
                  alt=""
                  className="absolute inset-0 h-full w-full object-cover object-[center_25%] mask-[linear-gradient(to_bottom,black,transparent)]"
                />
              )}
              <DrawerHeader className="relative min-h-44 justify-end">
                <DrawerTitle className="text-lg">{comic.title}</DrawerTitle>
                <DrawerDescription>{t('detail.readingState.title')}</DrawerDescription>
              </DrawerHeader>
            </div>
            <form onSubmit={handleSubmit} noValidate className="flex min-h-0 flex-1 flex-col">
              <div className="overflow-y-auto px-4">{renderFields()}</div>
              <DrawerFooter>
                {renderUpdateButton()}
                {renderRemoveButton()}
                <DrawerClose render={<Button type="button" size="sm" variant="ghost" />}>
                  {t('common.cancel')}
                </DrawerClose>
              </DrawerFooter>
            </form>
          </DrawerContent>
        </Drawer>
        {confirmRemoveDialog}
      </>
    );
  }

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle className="text-lg">{comic.title}</DialogTitle>
            <DialogDescription>{t('detail.readingState.title')}</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleSubmit} noValidate>
            <div className="flex gap-6">
              {comic.coverUrl ? (
                <img src={comic.coverUrl} alt="" className="aspect-2/3 w-2/5 self-start rounded-md object-cover" />
              ) : (
                <div className="aspect-2/3 w-2/5 self-start rounded-md bg-muted" />
              )}
              {renderFields()}
            </div>
            <DialogFooter className="sm:justify-between">
              {renderRemoveButton()}
              {renderUpdateButton()}
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
      {confirmRemoveDialog}
    </>
  );
}

export default EditReadingStateDialog;
