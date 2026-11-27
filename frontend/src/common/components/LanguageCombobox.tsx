import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { LANGUAGE_CODES } from '@/common/lib/languageCodes';
import { createLanguageNameFormatter } from '@/common/lib/languageName';
import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from '@/common/components/ui/combobox';

interface LanguageOption {
  value: string;
  label: string;
}

interface LanguageComboboxProps {
  id: string;
  /** Selected language code, or an empty string when none is selected. */
  value: string;
  onValueChange: (code: string) => void;
  placeholder: string;
  emptyMessage: string;
}

/**
 * Search-as-you-type picker over the curated `LANGUAGE_CODES`, with each language named in the UI's current
 * language. Texts come in as props so the component doesn't depend on any one feature's translation keys.
 */
function LanguageCombobox({ id, value, onValueChange, placeholder, emptyMessage }: Readonly<LanguageComboboxProps>) {
  const { i18n } = useTranslation();

  const languageOptions = useMemo(() => {
    const nameOf = createLanguageNameFormatter(i18n.language);
    return LANGUAGE_CODES.map((code) => ({ value: code, label: nameOf(code) })).sort((a, b) =>
      a.label.localeCompare(b.label, i18n.language),
    );
  }, [i18n.language]);

  const selectedLanguage = languageOptions.find((option) => option.value === value) ?? null;

  return (
    <Combobox
      items={languageOptions}
      value={selectedLanguage}
      onValueChange={(item) => onValueChange(item ? item.value : '')}
    >
      <ComboboxInput id={id} placeholder={placeholder} />
      <ComboboxContent>
        <ComboboxEmpty>{emptyMessage}</ComboboxEmpty>
        <ComboboxList>
          {(item: LanguageOption) => (
            <ComboboxItem key={item.value} value={item}>
              {item.label}
            </ComboboxItem>
          )}
        </ComboboxList>
      </ComboboxContent>
    </Combobox>
  );
}

export default LanguageCombobox;
