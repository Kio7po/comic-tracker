import { useTranslation } from 'react-i18next';
import { Activity, BookOpen, Layers } from 'lucide-react';
import { Card, CardContent } from '@/common/components/ui/card';
import type { Comic } from '@/services/comic/types';
import ReadingStateButton from './ReadingStateButton';

interface ComicCoverPanelProps {
  comic: Comic;
}

function ComicCoverPanel({ comic }: Readonly<ComicCoverPanelProps>) {
  const { t } = useTranslation();

  return (
    <div className="w-full shrink-0 lg:w-64">
      {/* Hidden on mobile - ComicDetailPage shows a small thumbnail next to the title there
          instead, so the full-size cover isn't repeated and doesn't eat the whole screen. */}
      <Card size="sm" className="hidden py-0 lg:block">
        {comic.coverUrl ? (
          <img src={comic.coverUrl} alt="" className="aspect-2/3 w-full object-cover" />
        ) : (
          <div className="aspect-2/3 w-full bg-muted" />
        )}
      </Card>
      <ReadingStateButton comic={comic} />
      <Card className="mt-4">
        <CardContent className="grid grid-cols-3 gap-4 text-sm sm:flex sm:flex-col sm:gap-3">
          <div className="flex flex-col items-center gap-1 text-center sm:flex-row sm:items-center sm:justify-between sm:text-left">
            <span className="flex items-center gap-1 text-xs text-muted-foreground sm:gap-1.5 sm:text-sm">
              <BookOpen className="size-3.5 sm:size-4" />
              {t('catalog.filters.type')}
            </span>
            <span className="font-medium text-foreground">
              {comic.mediaType ? t(`catalog.mediaType.${comic.mediaType}`) : t('detail.unknown')}
            </span>
          </div>
          <div className="flex flex-col items-center gap-1 text-center sm:flex-row sm:items-center sm:justify-between sm:text-left">
            <span className="flex items-center gap-1 text-xs text-muted-foreground sm:gap-1.5 sm:text-sm">
              <Activity className="size-3.5 sm:size-4" />
              {t('detail.publicationStatus')}
            </span>
            <span className="font-medium text-foreground">
              {comic.status ? t(`catalog.status.${comic.status}`) : t('detail.unknown')}
            </span>
          </div>
          <div className="flex flex-col items-center gap-1 text-center sm:flex-row sm:items-center sm:justify-between sm:text-left">
            <span className="flex items-center gap-1 text-xs text-muted-foreground sm:gap-1.5 sm:text-sm">
              <Layers className="size-3.5 sm:size-4" />
              {t('detail.chapters')}
            </span>
            <span className="font-medium text-foreground">{comic.chapters ?? t('detail.unknown')}</span>
          </div>
          {/* NSFW never actually has data yet (no active ComicMetadataProvider sets it) -
              commented out instead of removed so it's trivial to bring back once one does. */}
          {/* <div className="flex flex-col items-center gap-1 text-center sm:flex-row sm:items-center sm:justify-between sm:text-left">
            <span className="flex items-center gap-1 text-xs text-muted-foreground sm:gap-1.5 sm:text-sm">
              <ShieldAlert className="size-3.5 sm:size-4" />
              {t('catalog.filters.nsfw')}
            </span>
            <span className="font-medium text-foreground">
              {comic.nsfw ? t(`catalog.nsfw.${comic.nsfw}`) : t('detail.unknown')}
            </span>
          </div> */}
        </CardContent>
      </Card>
    </div>
  );
}

export default ComicCoverPanel;
