import { useTranslation } from 'react-i18next';
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
import type { ComicMediaType, ComicStatus, NsfwRating } from '@/services/comic/types';

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

interface SearchFiltersProps {
  type: ComicMediaType | undefined;
  status: ComicStatus | undefined;
  nsfw: NsfwRating | undefined;
  limit: number;
  onTypeChange: (value: ComicMediaType | undefined) => void;
  onStatusChange: (value: ComicStatus | undefined) => void;
  onNsfwChange: (value: NsfwRating | undefined) => void;
  onLimitChange: (value: number) => void;
}

function SearchFilters({
  type,
  status,
  nsfw,
  limit,
  onTypeChange,
  onStatusChange,
  onNsfwChange,
  onLimitChange,
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
  // No "any" option here: EXPLICIT is already the ceiling with no filtering applied
  // (see TenraiComicMetadataProvider.applyNsfwFilter), so it already means "unrestricted".
  const nsfwRatings = NSFW_RATINGS.map((value) => ({ value, label: t(`catalog.nsfw.${value}`) }));
  const limitOptions = LIMIT_OPTIONS.map((value) => ({ value: String(value), label: String(value) }));

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
    <div className="mt-3 flex flex-wrap items-center gap-3">
      <Select
        value={type ?? 'ALL'}
        items={mediaTypes}
        onValueChange={(value) => onTypeChange(value === 'ALL' ? undefined : (value as ComicMediaType))}
      >
        <SelectTrigger>
          <SelectValue/>
        </SelectTrigger>
        <SelectContent>
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
        <SelectTrigger>
          <SelectValue/>
        </SelectTrigger>
        <SelectContent>
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
        value={nsfw ?? 'EXPLICIT'}
        items={nsfwRatings}
        onValueChange={(value) => onNsfwChange(value === 'EXPLICIT' ? undefined : (value as NsfwRating))}
      >
        <SelectTrigger>
          <SelectValue/>
        </SelectTrigger>
        <SelectContent>
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
      <Separator orientation="vertical" className="hidden sm:block" />
      <Select
        value={String(limit)}
        items={selectLimitOptions}
        onValueChange={(value) => onLimitChange(Number(value))}
      >
        <SelectTrigger>
          <SelectValue/>
        </SelectTrigger>
        <SelectContent>
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
    </div>
  );
}

export default SearchFilters;
