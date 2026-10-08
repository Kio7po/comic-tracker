import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useMatches } from 'react-router';

const SITE_NAME = 'Manganamao';

// A route opts into a page title via its own `handle.title`:
// - a plain string: an i18n key, translated here so it stays reactive to language changes
//   (the route itself is built once outside React, it can't call t() directly).
// - a function: for a title that depends on loader data instead of translation (e.g. the comic's
//   own name on its detail page), given that match's own `loaderData`.
export interface RouteHandle {
  title?: string | ((data: unknown) => string);
}

// If it's a function, call it with the data, if not then it's a i18n key and must be translated.
function resolvePageTitle(t: (key: string) => string, handle: RouteHandle | undefined, data: unknown): string | undefined {
  if (!handle?.title) {
    return undefined;
  }
  if (typeof handle.title === 'function') {
    return handle.title(data);
  }
  return t(handle.title);
}

// Mounted once in Layout. Walks the matched route chain from the most specific match
// up, using the first one that declares a handle.title.
function useDocumentTitle() {
  const matches = useMatches();
  const { t, i18n } = useTranslation();

  useEffect(() => {
    const match = [...matches].reverse().find((candidate) => (candidate.handle as RouteHandle | undefined)?.title);
    const pageTitle = resolvePageTitle(t, match?.handle as RouteHandle | undefined, match?.loaderData);

    document.title = pageTitle ? `${pageTitle} | ${SITE_NAME}` : SITE_NAME;
    // i18n.language isn't read directly, but a language change re-renders this component with a
    // new t() identity - listed so the effect re-runs and retranslates the current page's title.
  }, [matches, t, i18n.language]);
}

export default useDocumentTitle;
