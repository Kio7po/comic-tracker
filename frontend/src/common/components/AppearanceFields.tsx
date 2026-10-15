import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Monitor, Moon, Sun } from 'lucide-react';
import { useTheme, type Theme } from '@/common/components/ThemeProvider';
import { Button } from '@/common/components/ui/button';
import { ButtonGroup } from '@/common/components/ui/button-group';
import { Field, FieldDescription, FieldGroup, FieldLabel } from '@/common/components/ui/field';
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/common/components/ui/select';
import { SUPPORTED_LANGUAGES, getStoredLanguage, setLanguage, type LanguagePreference } from '@/common/i18n';

const THEME_OPTIONS: { value: Theme; icon: typeof Sun; labelKey: string }[] = [
  { value: 'light', icon: Sun, labelKey: 'settings.appearance.light' },
  { value: 'dark', icon: Moon, labelKey: 'settings.appearance.dark' },
  { value: 'system', icon: Monitor, labelKey: 'settings.appearance.system' },
];

// Theme and language pickers. These are device-level preferences (not tied to the account), so the
// same fields are rendered wherever they're reachable: the settings tab, and the logged-out
// preferences dialog/page.
function AppearanceFields() {
  const { t } = useTranslation();
  const { theme, setTheme } = useTheme();
  const [preference, setPreference] = useState<LanguagePreference>(() => getStoredLanguage() ?? 'auto');

  // Each language is named in itself (not in the current UI language) so it stays findable
  // if the user lands on a language they can't read.
  // Intl lowercases language names in some locales ("español"), so the first letter is
  // capitalized to keep the list consistent.
  const languages: { value: LanguagePreference; label: string }[] = SUPPORTED_LANGUAGES.map((value) => {
    const name = new Intl.DisplayNames([value], { type: 'language' }).of(value) ?? value;
    return { value, label: name.charAt(0).toLocaleUpperCase(value) + name.slice(1) };
  });
  // Unlike the language names, 'auto' is translated.
  const options = [{ value: 'auto' as const, label: t('settings.appearance.languageAuto') }, ...languages];

  function handleLanguageChange(value: LanguagePreference) {
    setPreference(value);
    void setLanguage(value);
  }

  return (
    <FieldGroup>
      <Field>
        <FieldLabel>{t('settings.appearance.theme')}</FieldLabel>
        <ButtonGroup>
          {THEME_OPTIONS.map((option) => (
            <Button
              key={option.value}
              type="button"
              variant={theme === option.value ? 'default' : 'outline'}
              onClick={() => setTheme(option.value)}
            >
              <option.icon className="size-4" />
              {t(option.labelKey)}
            </Button>
          ))}
        </ButtonGroup>
        <FieldDescription>{t('settings.appearance.systemDescription')}</FieldDescription>
      </Field>
      <Field>
        <FieldLabel>{t('settings.appearance.language')}</FieldLabel>
        <Select
          value={preference}
          items={options}
          onValueChange={(value) => handleLanguageChange(value as LanguagePreference)}
        >
          <SelectTrigger className="w-full sm:w-fit">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectGroup>
              {options.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectGroup>
          </SelectContent>
        </Select>
        <FieldDescription>{t('settings.appearance.languageDescription')}</FieldDescription>
      </Field>
    </FieldGroup>
  );
}

export default AppearanceFields;
