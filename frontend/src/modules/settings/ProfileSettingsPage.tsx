import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useRouteLoaderData } from 'react-router';
import { updateProfile } from '@/services/user/api/user';
import type { UserResponse } from '@/services/user/types';
import { useAuth } from '@/common/components/AuthProvider';
import { displayNameInitials } from '@/common/lib/displayNameInitials';
import { LANGUAGE_CODES } from '@/common/lib/languageCodes';
import { createLanguageNameFormatter } from '@/common/lib/languageName';
import { Avatar, AvatarFallback, AvatarImage } from '@/common/components/ui/avatar';
import { Button } from '@/common/components/ui/button';
import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from '@/common/components/ui/combobox';
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/common/components/ui/field';
import { Input } from '@/common/components/ui/input';
import { Spinner } from '@/common/components/ui/spinner';
import { Textarea } from '@/common/components/ui/textarea';
import { toast } from '@/common/components/ui/toast';

interface Option {
  value: string;
  label: string;
}

interface FormState {
  displayName: string;
  biography: string;
  pictureUrl: string;
  locale: string;
}

type FormErrors = Partial<Record<keyof FormState, string>>;

// Matches @Size(max = 2048) on the backend's UserProfileRequestDto.biography.
const BIOGRAPHY_MAX_LENGTH = 2048;

const URL_PATTERN = /^https?:\/\/.+/i;

function toFormState(user: UserResponse): FormState {
  return {
    displayName: user.displayName,
    biography: user.biography ?? '',
    pictureUrl: user.pictureUrl ?? '',
    locale: user.locale ?? '',
  };
}

function validate(form: FormState): FormErrors {
  const errors: FormErrors = {};
  if (form.displayName.trim().length === 0) {
    errors.displayName = 'settings.profile.errors.displayNameRequired';
  }
  if (form.pictureUrl.trim().length > 0 && !URL_PATTERN.test(form.pictureUrl)) {
    errors.pictureUrl = 'settings.profile.errors.pictureUrlInvalid';
  }
  return errors;
}

function ProfileSettingsPage() {
  const { t, i18n } = useTranslation();
  const user = useRouteLoaderData('settings') as UserResponse;
  const { updateUser } = useAuth();

  const languageOptions = useMemo(() => {
    const nameOf = createLanguageNameFormatter(i18n.language);
    return LANGUAGE_CODES.map((code) => ({ value: code, label: nameOf(code) })).sort((a, b) =>
      a.label.localeCompare(b.label, i18n.language),
    );
  }, [i18n.language]);

  const [form, setForm] = useState<FormState>(() => toFormState(user));
  const [savedForm, setSavedForm] = useState<FormState>(() => toFormState(user));
  const [formErrors, setFormErrors] = useState<FormErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  function updateField<K extends keyof FormState>(field: K, value: FormState[K]) {
    setForm((previous) => ({ ...previous, [field]: value }));
    setFormErrors((errors) => ({ ...errors, [field]: undefined }));
  }

  const selectedLanguage = languageOptions.find((option) => option.value === form.locale) ?? null;
  const hasChanges =
    form.displayName.trim() !== savedForm.displayName ||
    form.biography.trim() !== savedForm.biography ||
    form.pictureUrl.trim() !== savedForm.pictureUrl ||
    form.locale !== savedForm.locale;

  async function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);
    setFormErrors({});

    const errors = validate(form);
    const isValid = Object.values(errors).every((error) => error === undefined);
    if (!isValid) {
      setFormErrors(errors);
      return;
    }

    setIsSaving(true);
    try {
      const trimmedBiography = form.biography.trim();
      const trimmedPictureUrl = form.pictureUrl.trim();
      const updated = await updateProfile({
        displayName: form.displayName.trim(),
        biography: trimmedBiography === '' ? undefined : trimmedBiography,
        pictureUrl: trimmedPictureUrl === '' ? undefined : trimmedPictureUrl,
        locale: form.locale === '' ? undefined : form.locale,
      });
      updateUser(updated);
      setForm(toFormState(updated));
      setSavedForm(toFormState(updated));
      toast.add({ title: t('settings.profile.success'), type: 'success' });
    } catch {
      setFormError(t('settings.profile.errors.generic'));
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <FieldGroup>
        <div className="flex items-center gap-4">
          <Avatar className="size-20">
            <AvatarImage src={form.pictureUrl.trim() || undefined} alt="" />
            <AvatarFallback className="text-3xl">
              {displayNameInitials(form.displayName || user.displayName)}
            </AvatarFallback>
          </Avatar>
          <Field data-invalid={!!formErrors.pictureUrl} className="flex-1">
            <div className="flex items-baseline justify-between gap-2">
              <FieldLabel htmlFor="settings-picture-url">{t('settings.profile.pictureUrl')}</FieldLabel>
              <FieldError className="text-xs">
                {formErrors.pictureUrl && t(formErrors.pictureUrl)}
              </FieldError>
            </div>
            <Input
              id="settings-picture-url"
              placeholder={t('settings.profile.pictureUrlPlaceholder')}
              value={form.pictureUrl}
              onChange={(event) => updateField('pictureUrl', event.target.value)}
            />
          </Field>
        </div>
        <Field data-invalid={!!formErrors.displayName}>
          <div className="flex items-baseline justify-between gap-2">
            <FieldLabel htmlFor="settings-display-name">
              {t('settings.profile.displayName')}
              <span className="text-destructive">*</span>
            </FieldLabel>
            <FieldError className="text-xs">
              {formErrors.displayName && t(formErrors.displayName)}
            </FieldError>
          </div>
          <Input
            id="settings-display-name"
            autoComplete="nickname"
            required
            value={form.displayName}
            onChange={(event) => updateField('displayName', event.target.value)}
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="settings-biography">{t('settings.profile.biography')}</FieldLabel>
          <Textarea
            id="settings-biography"
            placeholder={t('settings.profile.biographyPlaceholder')}
            value={form.biography}
            maxLength={BIOGRAPHY_MAX_LENGTH}
            onChange={(event) => updateField('biography', event.target.value)}
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="settings-locale">{t('settings.profile.locale')}</FieldLabel>
          <Combobox
            items={languageOptions}
            value={selectedLanguage}
            onValueChange={(item: Option | null) => updateField('locale', item ? item.value : '')}
          >
            <ComboboxInput
              id="settings-locale"
              placeholder={t('settings.profile.localePlaceholder')}
              showClear
            />
            <ComboboxContent>
              <ComboboxEmpty>{t('settings.profile.noLanguagesFound')}</ComboboxEmpty>
              <ComboboxList>
                {(item: Option) => (
                  <ComboboxItem key={item.value} value={item}>
                    {item.label}
                  </ComboboxItem>
                )}
              </ComboboxList>
            </ComboboxContent>
          </Combobox>
          <FieldDescription>{t('settings.profile.localeDescription')}</FieldDescription>
        </Field>
        <div className="flex flex-col gap-2">
          <div className="min-h-4">
            <FieldError className="text-xs">{formError}</FieldError>
          </div>
          <Button type="submit" disabled={isSaving || !hasChanges} className="self-start">
            {isSaving && <Spinner />}
            {isSaving ? t('settings.profile.saving') : t('settings.profile.save')}
          </Button>
        </div>
      </FieldGroup>
    </form>
  );
}

export default ProfileSettingsPage;
