import { useEffect, useRef, useState } from 'react';
import { Card } from '@/common/components/ui/card';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/common/components/ui/tooltip';
import type { ComicSearchResult } from '@/services/comic/types';

interface ComicCardProps {
  comic: ComicSearchResult;
}

function ComicCard({ comic }: Readonly<ComicCardProps>) {
  const titleRef = useRef<HTMLParagraphElement>(null);
  const [isTitleTruncated, setIsTitleTruncated] = useState(false);

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

  return (
    <div>
      <button type="button" className="block w-full">
        <Card size="sm" className="py-0 transition-[filter,scale] hover:brightness-110 hover:scale-[1.02]">
          {comic.coverUrl ? (
            <img src={comic.coverUrl} alt="" className="aspect-2/3 w-full object-cover" />
          ) : (
            <div className="aspect-2/3 w-full bg-muted" />
          )}
        </Card>
      </button>
      <Tooltip disabled={!isTitleTruncated}>
        <TooltipTrigger render={<p ref={titleRef} className="mt-2 truncate text-left text-sm font-medium" />}>
          {comic.title}
        </TooltipTrigger>
        <TooltipContent>{comic.title}</TooltipContent>
      </Tooltip>
    </div>
  );
}

export default ComicCard;
