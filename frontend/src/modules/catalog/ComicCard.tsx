import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { Card } from '@/common/components/ui/card';
import { Spinner } from '@/common/components/ui/spinner';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/common/components/ui/tooltip';
import { importComic } from '@/services/comic/api/catalog';
import type { ComicSearchResult } from '@/services/comic/types';

interface ComicCardProps {
  comic: ComicSearchResult;
}

function ComicCard({ comic }: Readonly<ComicCardProps>) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const titleRef = useRef<HTMLParagraphElement>(null);
  const [isTitleTruncated, setIsTitleTruncated] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    const titleElement = titleRef.current;
    if (!titleElement) return;

    const checkTruncation = () => {
      setIsTitleTruncated(titleElement.scrollWidth > titleElement.clientWidth);
    };

    checkTruncation();
    const resizeObserver = new ResizeObserver(checkTruncation);
    resizeObserver.observe(titleElement);
    return () => resizeObserver.disconnect();
  }, []);

  async function handleClick() {
    if (isImporting) return;
    setIsImporting(true);
    setHasError(false);
    try {
      const importedComic = await importComic(comic.sourceSlug, comic.externalId);
      navigate(`/comics/${importedComic.slug}`);
    } catch {
      setHasError(true);
      setIsImporting(false);
    }
  }

  return (
    <div>
      <button type="button" className="block w-full" disabled={isImporting} onClick={handleClick}>
        <Card
          size="sm"
          className="relative py-0 transition-[filter,scale] hover:brightness-110 hover:scale-[1.02]"
        >
          {comic.coverUrl ? (
            <img src={comic.coverUrl} alt="" className="aspect-2/3 w-full object-cover" />
          ) : (
            <div className="aspect-2/3 w-full bg-muted" />
          )}
          {isImporting && (
            <div className="absolute inset-0 flex items-center justify-center bg-background/70">
              <Spinner className="size-6" />
            </div>
          )}
        </Card>
      </button>
      <Tooltip disabled={!isTitleTruncated}>
        <TooltipTrigger render={<p ref={titleRef} className="mt-2 truncate text-left text-sm font-medium" />}>
          {comic.title}
        </TooltipTrigger>
        <TooltipContent>{comic.title}</TooltipContent>
      </Tooltip>
      {hasError && <p className="mt-1 text-xs text-destructive">{t('catalog.importError')}</p>}
    </div>
  );
}

export default ComicCard;
