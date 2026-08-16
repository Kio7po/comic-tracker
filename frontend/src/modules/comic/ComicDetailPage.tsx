import { useLoaderData } from 'react-router';
import type { LoaderFunctionArgs } from 'react-router';
import { getBySlug } from '@/services/comic/api/comic';
import ComicCoverPanel from './ComicCoverPanel';
import ComicHeader from './ComicHeader';
import ComicSynopsis from './ComicSynopsis';
import ComicReadingSources from './ComicReadingSources';
import ComicSidebarInfo from './ComicSidebarInfo';
import type { Comic } from '@/services/comic/types';

export function comicDetailLoader({ params, request }: LoaderFunctionArgs): Promise<Comic> {
  return getBySlug(params.slug!, { signal: request.signal });
}

function ComicDetailPage() {
  const comic = useLoaderData<typeof comicDetailLoader>();

  return (
    <div className="mx-auto max-w-6xl px-6 py-8">
      <div className="flex flex-col gap-8 lg:flex-row">
        <ComicCoverPanel comic={comic} />
        <div className="flex flex-1 flex-col gap-6">
          <ComicHeader comic={comic} />
          <div className="flex flex-col gap-6 lg:flex-row">
            <div className="flex flex-1 flex-col gap-6">
              <ComicSynopsis synopsis={comic.synopsis} />
              <ComicReadingSources comicSlug={comic.slug} />
            </div>
            <ComicSidebarInfo comic={comic} />
          </div>
        </div>
      </div>
    </div>
  );
}

export default ComicDetailPage;
