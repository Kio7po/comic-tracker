import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Badge } from '@/common/components/ui/badge';
import { Dialog, DialogContent, DialogTitle } from '@/common/components/ui/dialog';
import type { Comic } from '@/services/comic/types';

// Mobile only (lg:hidden - rendered alongside ComicDetailPage's lg:hidden/hidden lg:block
// siblings, ComicCoverPanel's cover and ComicHeader's Card) - a small, Card-less thumbnail next
// to the title/tags, instead of repeating the screen-filling cover the user already saw in the
// catalog/library card. Tapping it opens the cover full-size in a Dialog - desktop's own cover
// (ComicCoverPanel) is already large enough that it doesn't need the same escape hatch.
function ComicMobileHeader({ comic }: Readonly<{ comic: Comic }>) {
  const { t } = useTranslation();
  const [isCoverOpen, setIsCoverOpen] = useState(false);

  return (
    <>
      <div className="mb-4 flex items-start gap-4 lg:hidden">
        {comic.coverUrl ? (
          <button type="button" onClick={() => setIsCoverOpen(true)} aria-label={t('detail.viewCover')} className="shrink-0">
            <img src={comic.coverUrl} alt="" className="aspect-2/3 w-28 rounded-md object-cover" />
          </button>
        ) : (
          <div className="aspect-2/3 w-28 shrink-0 rounded-md bg-muted" />
        )}
        <div className="min-w-0 flex-1">
          <h1 className="text-xl font-semibold text-foreground">{comic.title}</h1>
          {comic.alternativeTitles.length > 0 && (
            <p className="mt-1 text-sm text-muted-foreground">{comic.alternativeTitles.join(' • ')}</p>
          )}
          {comic.genres.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-2">
              {comic.genres.map((genre) => (
                <Badge key={genre} variant="secondary">
                  {genre}
                </Badge>
              ))}
            </div>
          )}
        </div>
      </div>
      {comic.coverUrl && (
        <Dialog open={isCoverOpen} onOpenChange={setIsCoverOpen}>
          <DialogContent className="gap-0 bg-transparent p-0 ring-0 sm:max-w-md">
            <DialogTitle className="sr-only">{comic.title}</DialogTitle>
            <img src={comic.coverUrl} alt="" className="aspect-2/3 w-full rounded-xl object-cover" />
          </DialogContent>
        </Dialog>
      )}
    </>
  );
}

export default ComicMobileHeader;
