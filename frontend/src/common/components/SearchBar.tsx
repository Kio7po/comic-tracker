import { useEffect, useRef, useState } from 'react';
import { Search } from 'lucide-react';
import { Input } from '@/common/components/ui/input';

const DEFAULT_DEBOUNCE_MS = 400;

interface SearchBarProps {
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  debounceMs?: number;
}

function SearchBar({
  value,
  onChange,
  placeholder,
  debounceMs = DEFAULT_DEBOUNCE_MS,
}: Readonly<SearchBarProps>) {
  const [inputValue, setInputValue] = useState(value);
  const [previousValue, setPreviousValue] = useState(value);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  // If the value received from the parent has changed since the last render,
  // synchronize the input with it. This only happens when the parent updates the
  // value for some reason other than our own debounced onChange (e.g. resetting
  // the search), because otherwise we've already synchronized it.
  // Adjusting state during render like this avoids the extra render pass
  // an effect-based sync would cause.
  if (value !== previousValue) {
    setPreviousValue(value);
    setInputValue(value);
  }

  useEffect(() => {
    return () => clearTimeout(debounceRef.current);
  }, []);

  function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    const next = event.target.value;
    setInputValue(next);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => onChange(next), debounceMs);
  }

  return (
    <div className="relative">
      <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
      <Input
        type="search"
        placeholder={placeholder}
        className="pl-8"
        value={inputValue}
        onChange={handleChange}
      />
    </div>
  );
}

export default SearchBar;
