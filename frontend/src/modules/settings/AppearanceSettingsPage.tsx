import { useTranslation } from 'react-i18next';
import { Monitor, Moon, Sun } from 'lucide-react';
import { useTheme, type Theme } from '@/common/components/ThemeProvider';
import { Button } from '@/common/components/ui/button';
import { ButtonGroup } from '@/common/components/ui/button-group';
import { Field, FieldDescription, FieldGroup, FieldLabel } from '@/common/components/ui/field';

const THEME_OPTIONS: { value: Theme; icon: typeof Sun; labelKey: string }[] = [
  { value: 'light', icon: Sun, labelKey: 'settings.appearance.light' },
  { value: 'dark', icon: Moon, labelKey: 'settings.appearance.dark' },
  { value: 'system', icon: Monitor, labelKey: 'settings.appearance.system' },
];

function AppearanceSettingsPage() {
  const { t } = useTranslation();
  const { theme, setTheme } = useTheme();

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
    </FieldGroup>
  );
}

export default AppearanceSettingsPage;
