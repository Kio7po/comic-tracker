import { useTranslation } from 'react-i18next';
import { ArrowDownWideNarrow, ArrowUpNarrowWide, FilterX } from 'lucide-react';
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
import { Separator } from '@/common/components/ui/separator';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/common/components/ui/tooltip';
import type { SortDirection } from '@/common/api/SortDirection';
import type { ComicMediaType, ComicSearchSortField, ComicStatus, NsfwRating } from '@/services/comic/types';

const MEDIA_TYPES: ComicMediaType[] = [
  'MANGA',
  'MANHWA',
  'MANHUA',
  'WEBTOON',
  'COMIC',
  'NOVEL',
  'ONE_SHOT',
  'DOUJINSHI',
  /* 'OTHER', */
];

const STATUSES: ComicStatus[] = ['ONGOING', 'COMPLETED', 'HIATUS', 'CANCELLED', /* 'OTHER' */];

const NSFW_RATINGS: NsfwRating[] = ['NONE', 'SUGGESTIVE', 'EXPLICIT'];

export const LIMIT_OPTIONS = [6, 12, 24, 48];

export const SORT_FIELDS: ComicSearchSortField[] = ['RELEVANCE', 'TITLE', 'POPULARITY', 'RELEASE_DATE'];

interface SearchFiltersProps {
  type: ComicMediaType | undefined;
  status: ComicStatus | undefined;
  nsfw: NsfwRating;
  limit: number;
  sortBy: ComicSearchSortField;
  direction: SortDirection;
  onTypeChange: (value: ComicMediaType | undefined) => void;
  onStatusChange: (value: ComicStatus | undefined) => void;
  onNsfwChange: (value: NsfwRating) => void;
  onLimitChange: (value: number) => void;
  onSortByChange: (value: ComicSearchSortField) => void;
  onDirectionChange: (value: SortDirection) => void;
  isAtDefault: boolean;
  onReset: () => void;
  // Base UI's Select has a real bug combining this with a modal focus trap,
  // callers rendering these Selects inside a Drawer/Dialog must pass false.
  alignSelectWithTrigger?: boolean;
}

function SearchFilters({
  type,
  status,
  nsfw,
  limit,
  sortBy,
  direction,
  onTypeChange,
  onStatusChange,
  onNsfwChange,
  onLimitChange,
  onSortByChange,
  onDirectionChange,
  isAtDefault,
  onReset,
  alignSelectWithTrigger = true,
}: Readonly<SearchFiltersProps>) {
  const { t } = useTranslation();

  const mediaTypes = [
    {value: 'ALL', label: t('catalog.filters.allTypes')},
    ...MEDIA_TYPES.map((value) => ({value, label: t(`catalog.mediaType.${value}`)}))
  ];
  const statuses = [
    {value: 'ALL', label: t('catalog.filters.allStatuses')},
    ...STATUSES.map((value) => ({value, label: t(`catalog.status.${value}`)}))
  ];
  // EXPLICIT is the ceiling with no filtering applied server-side
  const nsfwRatings = NSFW_RATINGS.map((value) => ({ value, label: t(`catalog.nsfw.${value}`) }));
  const limitOptions = LIMIT_OPTIONS.map((value) => ({ value: String(value), label: String(value) }));
  const sortFields = SORT_FIELDS.map((value) => ({ value, label: t(`catalog.sort.${value}`) }));

  const selectLimitOptions = limitOptions.map(({ value, label }) => ({
    value,
    label: (
      <>
        <span className="text-muted-foreground">
          {t("catalog.filters.limit")}:
        </span>{" "}
        {label}
      </>
    ),
  }));

  return (
    <div className="mt-3 flex flex-col items-stretch gap-3 sm:flex-row sm:flex-wrap sm:items-center">
      <Select
        value={type ?? 'ALL'}
        items={mediaTypes}
        onValueChange={(value) => onTypeChange(value === 'ALL' ? undefined : (value as ComicMediaType))}
      >
        <SelectTrigger className="w-full sm:w-fit">
          <SelectValue/>
        </SelectTrigger>
        <SelectContent alignItemWithTrigger={alignSelectWithTrigger}>
          <SelectGroup>
            <SelectLabel>{t('catalog.filters.type')}</SelectLabel>
            {mediaTypes.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.label}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
      <Select
        value={status ?? 'ALL'}
        items={statuses}
        onValueChange={(value) => onStatusChange(value === 'ALL' ? undefined : (value as ComicStatus))}
      >
        <SelectTrigger className="w-full sm:w-fit">
          <SelectValue/>
        </SelectTrigger>
        <SelectContent alignItemWithTrigger={alignSelectWithTrigger}>
          <SelectGroup>
            <SelectLabel>{t('catalog.filters.status')}</SelectLabel>
            {statuses.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.label}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
      <Select
        value={nsfw}
        items={nsfwRatings}
        onValueChange={(value) => onNsfwChange(value as NsfwRating)}
      >
        <SelectTrigger className="w-full sm:w-fit">
          <SelectValue/>
        </SelectTrigger>
        <SelectContent alignItemWithTrigger={alignSelectWithTrigger}>
          <SelectGroup>
            <SelectLabel>{t('catalog.filters.nsfw')}</SelectLabel>
            {nsfwRatings.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.label}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
      <Separator orientation="vertical" />
      <ButtonGroup className="w-full sm:w-fit">
        <Select
          value={sortBy}
          items={sortFields}
          onValueChange={(value) => onSortByChange(value as ComicSearchSortField)}
        >
          <SelectTrigger className="flex-1 sm:w-fit sm:flex-none">
            <SelectValue/>
          </SelectTrigger>
          <SelectContent alignItemWithTrigger={alignSelectWithTrigger}>
            <SelectGroup>
              <SelectLabel>{t('catalog.filters.sortBy')}</SelectLabel>
              {sortFields.map((item) => (
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
                disabled={sortBy === 'RELEVANCE'}
                aria-label={direction === 'ASC' ? t('catalog.filters.sortAsc') : t('catalog.filters.sortDesc')}
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
          <TooltipContent>
            {direction === 'ASC' ? t('catalog.filters.sortAsc') : t('catalog.filters.sortDesc')}
          </TooltipContent>
        </Tooltip>
      </ButtonGroup>
      <Separator orientation="vertical" />
      <Select
        value={String(limit)}
        items={selectLimitOptions}
        onValueChange={(value) => onLimitChange(Number(value))}
      >
        <SelectTrigger className="w-full sm:w-fit">
          <SelectValue/>
        </SelectTrigger>
        <SelectContent alignItemWithTrigger={alignSelectWithTrigger}>
          <SelectGroup>
            <SelectLabel>{t('catalog.filters.limit')}</SelectLabel>
            {limitOptions.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.label}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
      {!isAtDefault && (
        <Button type="button" variant="ghost" onClick={onReset}>
          <FilterX className="size-4" />
          {t('catalog.filters.reset')}
        </Button>
      )}
    </div>
  );
}

export default SearchFilters;
