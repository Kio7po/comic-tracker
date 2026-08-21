import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BookmarkCheck, ChevronsLeft, ChevronsRight, Minus, Plus, Trash2 } from 'lucide-react';
import { remove, update } from '@/services/readingState/api/readingState';
import type { ReadingState, ReadingStateStatus } from '@/services/readingState/types';
import ConfirmDialog from '@/common/components/ConfirmDialog';
import { Button } from '@/common/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/common/components/ui/dialog';
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

interface EditReadingStateDialogProps {
  comic: ComicWithChapters;
  readingState: ReadingState;
  onUpdated: (readingState: ReadingState) => void;
  onRemoved: () => void;
}

function EditReadingStateDialog({
  comic,
  readingState,
  onUpdated,
  onRemoved,
}: Readonly<EditReadingStateDialogProps>) {
  const { t } = useTranslation();

  const [open, setOpen] = useState(false);
  const [status, setStatus] = useState<ReadingStateStatus>(readingState.status);
  const [chapters, setChapters] = useState(readingState.chapters);
  const [notes, setNotes] = useState(readingState.notes ?? '');
  const [isSaving, setIsSaving] = useState(false);
  const [isRemoving, setIsRemoving] = useState(false);
  const [isRemoveConfirmOpen, setIsRemoveConfirmOpen] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  function handleOpenChange(nextOpen: boolean) {
    setOpen(nextOpen);
    if (nextOpen) {
      setStatus(readingState.status);
      setChapters(readingState.chapters);
      setNotes(readingState.notes ?? '');
      setFormError(null);
    }
  }

  async function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);
    setIsSaving(true);
    try {
      const trimmedNotes = notes.trim();
      onUpdated(await update(comic.slug, { status, chapters, notes: trimmedNotes === '' ? undefined : trimmedNotes }));
      setOpen(false);
    } catch {
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
      setOpen(false);
    } catch {
      setFormError(t('detail.readingState.errors.generic'));
    } finally {
      setIsRemoving(false);
    }
  }

  const statusOptions = STATUSES.map((value) => ({ value, label: t(`detail.readingState.statuses.${value}`) }));
  const hasChanges =
    status !== readingState.status ||
    chapters !== readingState.chapters ||
    notes.trim() !== (readingState.notes ?? '');

  return (
    <>
      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogTrigger render={<Button variant="outline" className="mt-4 w-full" />}>
          <BookmarkCheck className="size-4" />
          {t('detail.editLibrary')}
        </DialogTrigger>
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle className="text-lg">{comic.title}</DialogTitle>
            <DialogDescription>{t('detail.readingState.title')}</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleSubmit} noValidate>
            <div className="flex flex-col gap-6 sm:flex-row">
              {comic.coverUrl ? (
                <img
                  src={comic.coverUrl}
                  alt=""
                  className="mx-auto aspect-2/3 w-40 self-start rounded-md object-cover sm:mx-0 sm:w-2/5"
                />
              ) : (
                <div className="mx-auto aspect-2/3 w-40 self-start rounded-md bg-muted sm:mx-0 sm:w-2/5" />
              )}
              <FieldGroup className="flex-1">
                <Field>
                  <FieldLabel htmlFor="reading-state-status">{t('detail.readingState.status')}</FieldLabel>
                  <Select
                    value={status}
                    items={statusOptions}
                    onValueChange={(value) => setStatus(value as ReadingStateStatus)}
                  >
                    <SelectTrigger id="reading-state-status" className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
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
                  <FieldLabel htmlFor="reading-state-chapters">{t('detail.readingState.chaptersRead')}</FieldLabel>
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
                      id="reading-state-chapters"
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
                  <FieldLabel htmlFor="reading-state-notes">{t('detail.readingState.notes')}</FieldLabel>
                  <Textarea
                    id="reading-state-notes"
                    placeholder={t('detail.readingState.notesPlaceholder')}
                    value={notes}
                    maxLength={NOTES_MAX_LENGTH}
                    onChange={(event) => setNotes(event.target.value)}
                  />
                </Field>
                <div className="min-h-4">
                  <FieldError className="text-xs">{formError}</FieldError>
                </div>
              </FieldGroup>
            </div>
            <DialogFooter className="sm:justify-between">
              <Button
                type="button"
                variant="destructive"
                disabled={isRemoving}
                onClick={() => setIsRemoveConfirmOpen(true)}
              >
                {isRemoving ? <Spinner /> : <Trash2 className="size-4" />}
                {isRemoving ? t('detail.readingState.removing') : t('detail.readingState.remove')}
              </Button>
              <Button type="submit" disabled={isSaving || !hasChanges}>
                {isSaving && <Spinner />}
                {isSaving ? t('detail.readingState.updating') : t('detail.readingState.update')}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
      <ConfirmDialog
        open={isRemoveConfirmOpen}
        onOpenChange={setIsRemoveConfirmOpen}
        title={t('detail.readingState.removeConfirmTitle')}
        description={t('detail.readingState.removeConfirmDescription')}
        confirmLabel={t('detail.readingState.remove')}
        isConfirming={isRemoving}
        onConfirm={handleRemove}
      />
    </>
  );
}

export default EditReadingStateDialog;
