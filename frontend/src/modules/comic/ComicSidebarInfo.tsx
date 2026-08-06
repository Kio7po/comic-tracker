import { useTranslation } from 'react-i18next';
import { Calendar, Database, Tag, Users } from 'lucide-react';
import { Badge } from '@/common/components/ui/badge';
import { Card, CardContent } from '@/common/components/ui/card';
import type { Comic } from '@/services/comic/types';

interface ComicSidebarInfoProps {
  comic: Comic;
}

function ComicSidebarInfo({ comic }: Readonly<ComicSidebarInfoProps>) {
  const { t } = useTranslation();

  return (
    <div className="w-full shrink-0 lg:w-56">
      <Card>
        <CardContent>
          <h2 className="flex items-center gap-1.5 text-lg font-semibold text-foreground">
            <Database />
            {t('detail.additionalInfo')}
          </h2>
          <div className="mt-3 flex flex-col gap-4 text-sm">
            {comic.authors.length > 0 && (
              <div>
                <p className="flex items-center gap-1.5 text-muted-foreground">
                  <Users className="size-4" />
                  {t('detail.authors')}
                </p>
                <p className="mt-1 font-medium text-foreground">{comic.authors.join(', ')}</p>
              </div>
            )}
            {comic.startDate && (
              <div>
                <p className="flex items-center gap-1.5 text-muted-foreground">
                  <Calendar className="size-4" />
                  {t('detail.startDate')}
                </p>
                <p className="mt-1 font-medium text-foreground">{comic.startDate}</p>
              </div>
            )}
            {comic.tags.length > 0 && (
              <div>
                <p className="flex items-center gap-1.5 text-muted-foreground">
                  <Tag className="size-4" />
                  {t('detail.tags')}
                </p>
                <div className="mt-1 flex flex-wrap gap-1.5">
                  {comic.tags.map((tag) => (
                    <Badge key={tag} variant="outline">
                      {'#'+tag}
                    </Badge>
                  ))}
                </div>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default ComicSidebarInfo;
