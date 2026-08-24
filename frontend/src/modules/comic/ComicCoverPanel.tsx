import { useTranslation } from 'react-i18next';
import { Activity, BookOpen, Layers, ShieldAlert } from 'lucide-react';
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
      <Card size="sm" className="py-0">
        {comic.coverUrl ? (
          <img src={comic.coverUrl} alt="" className="aspect-2/3 w-full object-cover" />
        ) : (
          <div className="aspect-2/3 w-full bg-muted" />
        )}
      </Card>
      <ReadingStateButton comic={comic} />
      <Card className="mt-4">
        <CardContent className="grid grid-cols-4 gap-4 text-sm sm:flex sm:flex-col sm:gap-3">
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
              {t('catalog.filters.status')}
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
          <div className="flex flex-col items-center gap-1 text-center sm:flex-row sm:items-center sm:justify-between sm:text-left">
            <span className="flex items-center gap-1 text-xs text-muted-foreground sm:gap-1.5 sm:text-sm">
              <ShieldAlert className="size-3.5 sm:size-4" />
              {t('catalog.filters.nsfw')}
            </span>
            <span className="font-medium text-foreground">
              {comic.nsfw ? t(`catalog.nsfw.${comic.nsfw}`) : t('detail.unknown')}
            </span>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default ComicCoverPanel;
