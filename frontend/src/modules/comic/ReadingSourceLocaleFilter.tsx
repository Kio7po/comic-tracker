import { useTranslation } from 'react-i18next';
import { Button } from '@/common/components/ui/button';
import {
  Combobox,
  ComboboxChip,
  ComboboxChips,
  ComboboxChipsInput,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxItem,
  ComboboxList,
  useComboboxAnchor,
} from '@/common/components/ui/combobox';
import { Label } from '@/common/components/ui/label';

export interface LocaleOption {
  value: string;
  label: string;
}

interface ReadingSourceLocaleFilterProps {
  options: LocaleOption[];
  selectedOptions: LocaleOption[];
  onSelectedOptionsChange: (options: LocaleOption[]) => void;
  isAtDefault: boolean;
  onReset: () => void;
}

function ReadingSourceLocaleFilter({
  options,
  selectedOptions,
  onSelectedOptionsChange,
  isAtDefault,
  onReset,
}: Readonly<ReadingSourceLocaleFilterProps>) {
  const { t } = useTranslation();
  const anchor = useComboboxAnchor();

  return (
    <div className="mt-4 flex flex-col gap-1.5">
      <div className="flex items-baseline justify-between gap-2">
        <Label htmlFor="reading-sources-locale-filter" className="text-sm font-normal text-muted-foreground">
          {t('detail.filterByLocale')}
        </Label>
        {!isAtDefault && (
          <Button type="button" variant="link" size="xs" className="h-auto p-0 text-xs" onClick={onReset}>
            {t('detail.resetLocaleFilter')}
          </Button>
        )}
      </div>
      <Combobox multiple items={options} value={selectedOptions} onValueChange={onSelectedOptionsChange}>
        <ComboboxChips ref={anchor}>
          {selectedOptions.map((option) => (
            <ComboboxChip key={option.value}>{option.label}</ComboboxChip>
          ))}
          <ComboboxChipsInput id="reading-sources-locale-filter" placeholder={t('detail.filterByLocalePlaceholder')} />
        </ComboboxChips>
        <ComboboxContent anchor={anchor}>
          <ComboboxEmpty>{t('detail.suggestSource.noLanguagesFound')}</ComboboxEmpty>
          <ComboboxList>
            {(option: LocaleOption) => (
              <ComboboxItem key={option.value} value={option}>
                {option.label}
              </ComboboxItem>
            )}
          </ComboboxList>
        </ComboboxContent>
      </Combobox>
    </div>
  );
}

export default ReadingSourceLocaleFilter;
