import { Badge } from '@/common/components/ui/badge';
import { Card, CardContent } from '@/common/components/ui/card';
import type { Comic } from '@/services/comic/types';

interface ComicHeaderProps {
  comic: Comic;
}

function ComicHeader({ comic }: Readonly<ComicHeaderProps>) {
  return (
    <Card>
      <CardContent>
        <h1 className="text-3xl font-semibold text-foreground">{comic.title}</h1>
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
      </CardContent>
    </Card>
  );
}

export default ComicHeader;
