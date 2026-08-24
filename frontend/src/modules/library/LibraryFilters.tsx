import { useTranslation } from 'react-i18next';
import { ArrowDownWideNarrow, ArrowUpNarrowWide } from 'lucide-react';
import type { ComicMediaType, ComicStatus, NsfwRating } from '@/services/comic/types';
import type { ReadingStateStatus } from '@/services/readingState/types';
import type { SortDirection } from '@/common/api/SortDirection';
import { Button } from '@/common/components/ui/button';
import { ButtonGroup } from '@/common/components/ui/button-group';
import { Checkbox } from '@/common/components/ui/checkbox';
import { Label } from '@/common/components/ui/label';
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
import { Separator } from '@/common/components/ui/separator';

export type LibrarySortField =
  | 'TITLE'
  | 'RELEASE_DATE'
  | 'TOTAL_CHAPTERS'
  | 'UNREAD_CHAPTERS'
  | 'CREATED_AT'
  | 'UPDATED_AT';

const READING_STATUSES: ReadingStateStatus[] = ['READING', 'COMPLETED', 'ON_HOLD', 'PLAN_TO_READ', 'DROPPED'];
const MEDIA_TYPES: ComicMediaType[] = [
  'MANGA',
  'MANHWA',
  'MANHUA',
  'WEBTOON',
  'COMIC',
  'NOVEL',
  'ONE_SHOT',
  'DOUJINSHI',
];
const PUBLICATION_STATUSES: ComicStatus[] = ['ONGOING', 'COMPLETED', 'HIATUS', 'CANCELLED'];
// No "any" sentinel here, same as catalog's own nsfw filter: EXPLICIT is already the ceiling
// (every entry passes), so it doubles as "unrestricted".
const NSFW_RATINGS: NsfwRating[] = ['NONE', 'SUGGESTIVE', 'EXPLICIT'];
export const SORT_FIELDS: LibrarySortField[] = [
  'TITLE',
  'RELEASE_DATE',
  'TOTAL_CHAPTERS',
  'UNREAD_CHAPTERS',
  'CREATED_AT',
  'UPDATED_AT',
];

interface LibraryFiltersProps {
  readingStatus: ReadingStateStatus | undefined;
  pendingOnly: boolean;
  mediaType: ComicMediaType | undefined;
  publicationStatus: ComicStatus | undefined;
  nsfw: NsfwRating | undefined;
  sortField: LibrarySortField;
  sortDirection: SortDirection;
  onReadingStatusChange: (value: ReadingStateStatus | undefined) => void;
  onPendingOnlyChange: (value: boolean) => void;
  onMediaTypeChange: (value: ComicMediaType | undefined) => void;
  onPublicationStatusChange: (value: ComicStatus | undefined) => void;
  onNsfwChange: (value: NsfwRating | undefined) => void;
  onSortFieldChange: (value: LibrarySortField) => void;
  onSortDirectionChange: (value: SortDirection) => void;
  // Base UI's Select has a real bug combining this with a modal focus trap,
  // callers rendering these Selects inside a Drawer/Dialog must pass false.
  alignSelectWithTrigger?: boolean;
}

function LibraryFilters({
  readingStatus,
  pendingOnly,
  mediaType,
  publicationStatus,
  nsfw,
  sortField,
  sortDirection,
  onReadingStatusChange,
  onPendingOnlyChange,
  onMediaTypeChange,
  onPublicationStatusChange,
  onNsfwChange,
  onSortFieldChange,
  onSortDirectionChange,
  alignSelectWithTrigger = true,
}: Readonly<LibraryFiltersProps>) {
  const { t } = useTranslation();

  const readingStatuses = [
    { value: 'ALL', label: t('library.filters.allStatuses') },
    ...READING_STATUSES.map((value) => ({ value, label: t(`detail.readingState.statuses.${value}`) })),
  ];
  const mediaTypes = [
    { value: 'ALL', label: t('catalog.filters.allTypes') },
    ...MEDIA_TYPES.map((value) => ({ value, label: t(`catalog.mediaType.${value}`) })),
  ];
  const publicationStatuses = [
    { value: 'ALL', label: t('catalog.filters.allStatuses') },
    ...PUBLICATION_STATUSES.map((value) => ({ value, label: t(`catalog.status.${value}`) })),
  ];
  const nsfwRatings = NSFW_RATINGS.map((value) => ({ value, label: t(`catalog.nsfw.${value}`) }));
  const sortFields = SORT_FIELDS.map((value) => ({ value, label: t(`library.sort.${value}`) }));

  return (
    <div className="mt-3 flex flex-wrap items-center gap-3">
      <Select
        value={readingStatus ?? 'ALL'}
        items={readingStatuses}
        onValueChange={(value) => onReadingStatusChange(value === 'ALL' ? undefined : (value as ReadingStateStatus))}
      >
        <SelectTrigger>
          <SelectValue />
        </SelectTrigger>
        <SelectContent alignItemWithTrigger={alignSelectWithTrigger}>
          <SelectGroup>
            <SelectLabel>{t('library.filters.status')}</SelectLabel>
            {readingStatuses.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.label}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
      <Select
        value={mediaType ?? 'ALL'}
        items={mediaTypes}
        onValueChange={(value) => onMediaTypeChange(value === 'ALL' ? undefined : (value as ComicMediaType))}
      >
        <SelectTrigger>
          <SelectValue />
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
        value={publicationStatus ?? 'ALL'}
        items={publicationStatuses}
        onValueChange={(value) => onPublicationStatusChange(value === 'ALL' ? undefined : (value as ComicStatus))}
      >
        <SelectTrigger>
          <SelectValue />
        </SelectTrigger>
        <SelectContent alignItemWithTrigger={alignSelectWithTrigger}>
          <SelectGroup>
            <SelectLabel>{t('catalog.filters.status')}</SelectLabel>
            {publicationStatuses.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.label}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
      <Select
        value={nsfw ?? 'EXPLICIT'}
        items={nsfwRatings}
        onValueChange={(value) => onNsfwChange(value === 'EXPLICIT' ? undefined : (value as NsfwRating))}
      >
        <SelectTrigger>
          <SelectValue />
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
      <div className="flex items-center gap-2.5">
        <Checkbox
          id="library-pending-only"
          checked={pendingOnly}
          onCheckedChange={(checked) => onPendingOnlyChange(checked === true)}
        />
        <Label htmlFor="library-pending-only" className="text-sm font-normal text-foreground">
          {t('library.filters.pendingOnly')}
        </Label>
      </div>
      <Separator orientation='vertical'/>
      <ButtonGroup>
        <Select
          value={sortField}
          items={sortFields}
          onValueChange={(value) => onSortFieldChange(value as LibrarySortField)}
        >
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent alignItemWithTrigger={alignSelectWithTrigger}>
            <SelectGroup>
              <SelectLabel>{t('library.filters.sortBy')}</SelectLabel>
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
                aria-label={sortDirection === 'ASC' ? t('library.filters.sortAsc') : t('library.filters.sortDesc')}
                onClick={() => onSortDirectionChange(sortDirection === 'ASC' ? 'DESC' : 'ASC')}
              />
            }
          >
            {sortDirection === 'ASC' ? (
              <ArrowUpNarrowWide className="size-4" />
            ) : (
              <ArrowDownWideNarrow className="size-4" />
            )}
          </TooltipTrigger>
          <TooltipContent>
            {sortDirection === 'ASC' ? t('library.filters.sortAsc') : t('library.filters.sortDesc')}
          </TooltipContent>
        </Tooltip>
      </ButtonGroup>
    </div>
  );
}

export default LibraryFilters;
