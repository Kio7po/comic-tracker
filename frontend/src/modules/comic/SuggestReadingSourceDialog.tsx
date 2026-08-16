import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { TriangleAlert } from 'lucide-react';
import { submit } from '@/services/source/api/readingEntry';
import { findSelectable } from '@/services/source/api/readingSource';
import type { ComicReadingEntry } from '@/services/source/types';
import { ApiError } from '@/common/api/ApiError';
import { ProblemType } from '@/common/api/ProblemType';
import { Button } from '@/common/components/ui/button';
import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from '@/common/components/ui/combobox';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/common/components/ui/dialog';
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/common/components/ui/field';
import { Input } from '@/common/components/ui/input';
import { Spinner } from '@/common/components/ui/spinner';
import { Separator } from '@/common/components/ui/separator';

interface Option {
  value: string;
  label: string;
}

interface SourceOption extends Option {
  isPending: boolean;
}

// Language of the translation, not a specific country/region, hence no ISO 3166 region codes here
// even though the backend's ValidLocale also accepts them (e.g. "es-ES").
const LANGUAGE_CODES = [
  'en', 'es', 'fr', 'de', 'it', 'pt', 'ja', 'ko', 'zh', 'ru',
  'ar', 'hi', 'th', 'vi', 'id', 'tr', 'pl', 'nl', 'sv', 'uk',
];

type Mode = 'existing' | 'new';

interface FormState {
  sourceId: string;
  sourceName: string;
  sourceUrl: string;
  url: string;
  locale: string;
}

type FormErrors = Partial<Record<keyof FormState, string>>;

const EMPTY_FORM: FormState = { sourceId: '', sourceName: '', sourceUrl: '', url: '', locale: '' };

const URL_PATTERN = /^https?:\/\/.+/i;

function validate(mode: Mode, form: FormState): FormErrors {
  const errors: FormErrors = {};

  if (mode === 'existing' && !form.sourceId) {
    errors.sourceId = 'detail.suggestSource.errors.sourceRequired';
  }
  if (mode === 'new') {
    if (form.sourceName.trim().length === 0) errors.sourceName = 'detail.suggestSource.errors.sourceNameRequired';
    if (!URL_PATTERN.test(form.sourceUrl)) errors.sourceUrl = 'detail.suggestSource.errors.urlInvalid';
  }
  if (!URL_PATTERN.test(form.url)) errors.url = 'detail.suggestSource.errors.urlInvalid';
  if (!form.locale) errors.locale = 'detail.suggestSource.errors.localeRequired';

  return errors;
}

interface SuggestReadingSourceDialogProps {
  comicSlug: string;
  onEntrySubmitted?: (entry: ComicReadingEntry) => void;
}

function SuggestReadingSourceDialog({ comicSlug, onEntrySubmitted }: Readonly<SuggestReadingSourceDialogProps>) {
  const { t, i18n } = useTranslation();

  const languageOptions = useMemo(() => {
    const displayNames = new Intl.DisplayNames([i18n.language], { type: 'language' });
    return LANGUAGE_CODES.map((code) => ({ value: code, label: displayNames.of(code) ?? code })).sort((a, b) =>
      a.label.localeCompare(b.label, i18n.language),
    );
  }, [i18n.language]);

  const [sourceOptions, setSourceOptions] = useState<SourceOption[]>([]);
  const [isLoadingSources, setIsLoadingSources] = useState(true);
  const [hasSourceLoadError, setHasSourceLoadError] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    findSelectable({ signal: controller.signal })
      .then((sources) => {
        setSourceOptions(
          sources.map((source) => ({
            value: String(source.id),
            label: source.name,
            isPending: source.status === 'PENDING',
          })),
        );
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return;
        }
        setHasSourceLoadError(true);
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setIsLoadingSources(false);
        }
      });

    return () => {
      controller.abort();
    };
  }, []);

  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState<Mode>('existing');
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [formErrors, setFormErrors] = useState<FormErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  function updateField<K extends keyof FormState>(field: K, value: FormState[K]) {
    setForm((previous) => ({ ...previous, [field]: value }));
    setFormErrors((errors) => ({ ...errors, [field]: undefined }));
  }

  function handleSourceUrlChange(value: string) {
    try {
      updateField('sourceUrl', new URL(value).origin);
    } catch {
      // Not a parseable URL yet, store as typed, submit-time validation will flag it if it stays that way.
      updateField('sourceUrl', value);
    }
  }

  function switchMode(nextMode: Mode) {
    setMode(nextMode);
    setFormErrors({});
  }

  function handleOpenChange(nextOpen: boolean) {
    setOpen(nextOpen);
    if (!nextOpen) {
      setForm(EMPTY_FORM);
      setFormErrors({});
      setFormError(null);
      setIsSuccess(false);
      setMode('existing');
    }
  }

  async function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);

    const errors = validate(mode, form);
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await submit(
        comicSlug,
        mode === 'existing'
          ? { sourceId: Number(form.sourceId), url: form.url.trim(), locale: form.locale.trim() }
          : {
              sourceName: form.sourceName.trim(),
              sourceUrl: form.sourceUrl.trim(),
              url: form.url.trim(),
              locale: form.locale.trim(),
            },
      );
      if (mode === 'new') {
        const newSource: SourceOption = {
          value: String(response.source.id),
          label: response.source.name,
          isPending: response.source.status === 'PENDING',
        };
        setSourceOptions((previous) => [...previous, newSource].sort((a, b) => a.label.localeCompare(b.label)));
      }
      onEntrySubmitted?.(response);
      setIsSuccess(true);
    } catch (error) {
      if (error instanceof ApiError && error.type === ProblemType.DUPLICATE_READING_ENTRY) {
        setFormErrors((previous) => ({ ...previous, url: 'detail.suggestSource.errors.duplicateEntry' }));
      } else if (error instanceof ApiError && error.type === ProblemType.DUPLICATE_READING_SOURCE) {
        setFormErrors((previous) => ({ ...previous, sourceUrl: 'detail.suggestSource.errors.duplicateSource' }));
      } else {
        setFormError(t('detail.suggestSource.errors.generic'));
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  const selectedSource = sourceOptions.find((option) => option.value === form.sourceId) ?? null;
  const selectedLanguage = languageOptions.find((option) => option.value === form.locale) ?? null;

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger render={<Button variant="link" size="sm" />}>
        {'+ '+t('detail.suggestSource.trigger')}
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="text-lg">{t('detail.suggestSource.title')}</DialogTitle>
          <DialogDescription>{t('detail.suggestSource.description')}</DialogDescription>
        </DialogHeader>
        {isSuccess ? (
          <p className="text-sm text-foreground">{t('detail.suggestSource.success')}</p>
        ) : (
          <form onSubmit={handleSubmit} noValidate>
            <FieldGroup>
              {mode === 'existing' ? (
                <Field data-invalid={!!formErrors.sourceId}>
                  <div className="flex items-baseline justify-between gap-2">
                    <FieldLabel htmlFor="suggest-source-search">{t('detail.suggestSource.source')}</FieldLabel>
                    <FieldError className="text-xs">{formErrors.sourceId && t(formErrors.sourceId)}</FieldError>
                  </div>
                  <Combobox
                    items={sourceOptions}
                    value={selectedSource}
                    onValueChange={(item) => updateField('sourceId', item ? item.value : '')}
                  >
                    <ComboboxInput
                      id="suggest-source-search"
                      disabled={isLoadingSources}
                      placeholder={
                        isLoadingSources
                          ? t('detail.suggestSource.loadingSources')
                          : t('detail.suggestSource.sourcePlaceholder')
                      }
                    />
                    <ComboboxContent>
                      <ComboboxEmpty>
                        {hasSourceLoadError
                          ? t('detail.suggestSource.sourcesLoadError')
                          : t('detail.suggestSource.noSourcesFound')}
                      </ComboboxEmpty>
                      <ComboboxList>
                        {(item: SourceOption) => (
                          <ComboboxItem key={item.value} value={item}>
                            <span className="flex items-center gap-1.5">
                              {item.label}
                              {item.isPending && (
                                <span className="inline-flex text-amber-500" title={t('detail.pendingTooltip')}>
                                  <TriangleAlert className="size-3.5" />
                                </span>
                              )}
                            </span>
                          </ComboboxItem>
                        )}
                      </ComboboxList>
                    </ComboboxContent>
                  </Combobox>
                  <FieldDescription>
                    {t('detail.suggestSource.notListed')}{' '}
                    <button
                      type="button"
                      className="text-primary underline-offset-4 hover:underline"
                      onClick={() => switchMode('new')}
                    >
                      {t('detail.suggestSource.proposeNew')}
                    </button>
                  </FieldDescription>
                </Field>
              ) : (
                <>
                  <Field data-invalid={!!formErrors.sourceName}>
                    <div className="flex items-baseline justify-between gap-2">
                      <FieldLabel htmlFor="suggest-source-name">{t('detail.suggestSource.sourceName')}</FieldLabel>
                      <FieldError className="text-xs">
                        {formErrors.sourceName && t(formErrors.sourceName)}
                      </FieldError>
                    </div>
                    <Input
                      id="suggest-source-name"
                      placeholder={t('detail.suggestSource.sourceNamePlaceholder')}
                      value={form.sourceName}
                      onChange={(event) => updateField('sourceName', event.target.value)}
                    />
                  </Field>
                  <Field data-invalid={!!formErrors.sourceUrl}>
                    <div className="flex items-baseline justify-between gap-2">
                      <FieldLabel htmlFor="suggest-source-url">{t('detail.suggestSource.sourceUrl')}</FieldLabel>
                      <FieldError className="text-xs">{formErrors.sourceUrl && t(formErrors.sourceUrl)}</FieldError>
                    </div>
                    <Input
                      id="suggest-source-url"
                      placeholder={t('detail.suggestSource.sourceUrlPlaceholder')}
                      value={form.sourceUrl}
                      onChange={(event) => handleSourceUrlChange(event.target.value)}
                    />
                    <FieldDescription>
                      {t('detail.suggestSource.alreadyExists')}{' '}
                      <button
                        type="button"
                        className="text-primary underline-offset-4 hover:underline"
                        onClick={() => switchMode('existing')}
                      >
                        {t('detail.suggestSource.pickExisting')}
                      </button>
                    </FieldDescription>
                  </Field>
                </>
              )}
              <Separator/>
              <Field data-invalid={!!formErrors.url}>
                <div className="flex items-baseline justify-between gap-2">
                  <FieldLabel htmlFor="suggest-entry-url">{t('detail.suggestSource.entryUrl')}</FieldLabel>
                  <FieldError className="text-xs">{formErrors.url && t(formErrors.url)}</FieldError>
                </div>
                <Input
                  id="suggest-entry-url"
                  placeholder={t('detail.suggestSource.entryUrlPlaceholder')}
                  value={form.url}
                  onChange={(event) => updateField('url', event.target.value)}
                />
              </Field>
              <Field data-invalid={!!formErrors.locale}>
                <div className="flex items-baseline justify-between gap-2">
                  <FieldLabel htmlFor="suggest-entry-locale">{t('detail.suggestSource.locale')}</FieldLabel>
                  <FieldError className="text-xs">{formErrors.locale && t(formErrors.locale)}</FieldError>
                </div>
                <Combobox
                  items={languageOptions}
                  value={selectedLanguage}
                  onValueChange={(item) => updateField('locale', item ? item.value : '')}
                >
                  <ComboboxInput
                    id="suggest-entry-locale"
                    placeholder={t('detail.suggestSource.localePlaceholder')}
                  />
                  <ComboboxContent>
                    <ComboboxEmpty>{t('detail.suggestSource.noLanguagesFound')}</ComboboxEmpty>
                    <ComboboxList>
                      {(item: Option) => (
                        <ComboboxItem key={item.value} value={item}>
                          {item.label}
                        </ComboboxItem>
                      )}
                    </ComboboxList>
                  </ComboboxContent>
                </Combobox>
              </Field>
              <div className="min-h-4">
                <FieldError className="text-xs">{formError}</FieldError>
              </div>
            </FieldGroup>
            <DialogFooter>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting && <Spinner />}
                {isSubmitting ? t('detail.suggestSource.submitting') : t('detail.suggestSource.submit')}
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}

export default SuggestReadingSourceDialog;