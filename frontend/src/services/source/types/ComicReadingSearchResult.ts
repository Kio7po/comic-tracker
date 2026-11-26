export interface ComicReadingSearchResult {
  externalId: string | null;
  title: string;
  url: string | null;
  coverUrl: string | null;
}

export interface ComicReadingSearchListing {
  providerKey: string;
  results: ComicReadingSearchResult[];
}
