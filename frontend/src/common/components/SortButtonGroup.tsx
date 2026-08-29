import { useTranslation } from 'react-i18next';
import { ArrowDownWideNarrow, ArrowUpNarrowWide } from 'lucide-react';
import type { SortDirection } from '@/common/api/SortDirection';
import { Button } from '@/common/components/ui/button';
import { ButtonGroup } from '@/common/components/ui/button-group';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@/common/components/ui/select';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/common/components/ui/tooltip';

interface SortButtonGroupProps<T extends string> {
  items: { value: T; label: string }[];
  value: T;
  onValueChange: (value: T) => void;
  direction: SortDirection;
  onDirectionChange: (value: SortDirection) => void;
  // Some sort fields have no direction semantics (e.g. catalog search's RELEVANCE) - the toggle
  // stays mounted but disabled rather than unmounting, so the ButtonGroup's width doesn't change.
  directionDisabled?: boolean;
  // Base UI's Select has a real bug combining this with a modal focus trap,
  // callers rendering this inside a Drawer/Dialog must pass false.
  alignSelectWithTrigger?: boolean;
  className?: string;
}

function SortButtonGroup<T extends string>({
  items,
  value,
  onValueChange,
  direction,
  onDirectionChange,
  directionDisabled = false,
  alignSelectWithTrigger = true,
  className,
}: Readonly<SortButtonGroupProps<T>>) {
  const { t } = useTranslation();
  const directionLabel = direction === 'ASC' ? t('common.sort.asc') : t('common.sort.desc');

  return (
    <ButtonGroup className={className}>
      <Select value={value} items={items} onValueChange={(newValue) => onValueChange(newValue as T)}>
        <SelectTrigger className="flex-1 sm:w-fit sm:flex-none">
          <SelectValue />
        </SelectTrigger>
        <SelectContent alignItemWithTrigger={alignSelectWithTrigger}>
          <SelectGroup>
            <SelectLabel>{t('common.sort.by')}</SelectLabel>
            {items.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.label}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
      <Tooltip>
        <TooltipTrigger
          render={
            <Button
              type="button"
              variant="outline"
              size="icon"
              disabled={directionDisabled}
              aria-label={directionLabel}
              onClick={() => onDirectionChange(direction === 'ASC' ? 'DESC' : 'ASC')}
            />
          }
        >
          {direction === 'ASC' ? (
            <ArrowUpNarrowWide className="size-4" />
          ) : (
            <ArrowDownWideNarrow className="size-4" />
          )}
        </TooltipTrigger>
        <TooltipContent>{directionLabel}</TooltipContent>
      </Tooltip>
    </ButtonGroup>
  );
}

export default SortButtonGroup;
