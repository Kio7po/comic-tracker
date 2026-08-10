import { useTranslation } from 'react-i18next';
import { ExternalLink } from 'lucide-react';
import { Badge } from '@/common/components/ui/badge';
import { Card, CardContent } from '@/common/components/ui/card';

// Mock only: ComicReadingSource/ComicReadingEntry aren't implemented on the backend yet
// (see docs/TFG.md "Fuentes colaborativas").
interface MockReadingSource {
  siteName: string;
  domain: string;
  url: string;
  language: string;
  chapters: number;
}

const MOCK_READING_SOURCES: MockReadingSource[] = [
  { siteName: 'MangaPlus', domain: 'mangaplus.shueisha.co.jp', url: '#', language: 'EN', chapters: 374 },
  { siteName: 'MangaDex', domain: 'mangadex.org', url: '#', language: 'ES', chapters: 370 },
  { siteName: 'TuMangaOnline', domain: 'zonatmo.com', url: '#', language: 'ES', chapters: 374 },
];

function faviconUrl(domain: string): string {
  return `https://www.google.com/s2/favicons?domain=${domain}&sz=32`;
}

function ComicReadingSources() {
  const { t } = useTranslation();

  return (
    <Card>
      <CardContent>
        <h2 className="text-lg font-semibold text-foreground">{t('detail.readingSources')}</h2>
        <ul className="mt-2 flex flex-col gap-2">
          {MOCK_READING_SOURCES.map((source) => (
            <li key={source.siteName}>
              <a
                href={source.url}
                className="flex items-center justify-between rounded-md border border-border px-3 py-2 text-sm transition-colors hover:bg-muted"
              >
                <span className="flex items-center gap-2 font-medium text-foreground">
                  <img src={faviconUrl(source.domain)} alt="" className="size-6 rounded-xs" />
                  {source.siteName}
                </span>
                <span className="flex items-center gap-2 text-muted-foreground">
                  <Badge variant="outline">{source.language}</Badge>
                  <span>
                    {t('detail.chapters')} {source.chapters}
                  </span>
                  <ExternalLink className="size-4" />
                </span>
              </a>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}

export default ComicReadingSources;
