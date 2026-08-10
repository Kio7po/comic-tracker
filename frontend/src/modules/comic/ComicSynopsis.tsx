import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ChevronDown, ChevronUp } from 'lucide-react';
import { Card, CardContent } from '@/common/components/ui/card';
import { cn } from '@/common/lib/utils';

interface ComicSynopsisProps {
  synopsis: string | null;
}

function ComicSynopsis({ synopsis }: Readonly<ComicSynopsisProps>) {
  const { t } = useTranslation();
  const textRef = useRef<HTMLParagraphElement>(null);
  const [isExpanded, setIsExpanded] = useState(false);
  const [canExpand, setCanExpand] = useState(false);

  // Check if synopsis length visible is smaller than the total length.
  // If so we allow it to expand and contract.
  useEffect(() => {
    const textElement = textRef.current;
    if (!textElement || isExpanded) return;

    const checkOverflow = () => {
      setCanExpand(textElement.scrollHeight > textElement.clientHeight);
    };

    checkOverflow();
    const resizeObserver = new ResizeObserver(checkOverflow);
    resizeObserver.observe(textElement);
    return () => resizeObserver.disconnect();
  }, [isExpanded]);

  return (
    <Card>
      <CardContent>
        <h2 className="text-lg font-semibold text-foreground">{t('detail.synopsis')}</h2>
        <div className="relative mt-2">
          <p
            ref={textRef}
            className={cn('text-sm text-muted-foreground', !isExpanded && 'max-h-28 sm:max-h-none overflow-hidden')}
          >
            {synopsis ?? t('detail.noSynopsis')}
          </p>
          {!isExpanded && canExpand && (
            <div className="pointer-events-none absolute inset-x-0 bottom-0 h-20 bg-linear-to-t from-card to-transparent" />
          )}
        </div>
        {canExpand && (
          <button
            type="button"
            aria-label={isExpanded ? t('detail.collapseSynopsis') : t('detail.expandSynopsis')}
            onClick={() => setIsExpanded((expanded) => !expanded)}
            className="mt-1 flex w-full items-center justify-center text-muted-foreground hover:text-foreground"
          >
            {isExpanded ? <ChevronUp className="size-4" /> : <ChevronDown className="size-4" />}
          </button>
        )}
      </CardContent>
    </Card>
  );
}

export default ComicSynopsis;
