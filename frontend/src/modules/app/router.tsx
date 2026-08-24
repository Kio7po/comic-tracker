import { createBrowserRouter } from 'react-router';
import Layout from '@/common/components/Layout';
import RouteErrorPage from '@/common/components/RouteErrorPage';
import SearchPage from '@/modules/catalog/SearchPage';
import LibraryPage from '@/modules/library/LibraryPage';
import ComicDetailPage, { comicDetailLoader } from '@/modules/comic/ComicDetailPage';
import RegisterPage from '@/modules/auth/RegisterPage';
import LoginPage from '@/modules/auth/LoginPage';
import ModerationPage from '@/modules/moderation/ModerationPage';
import { requireAuth, requireRole } from './routeGuards';
import Home from './Home';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'catalog', element: <SearchPage /> },
      {
        path: 'library',
        element: <LibraryPage />,
        loader: ({ request }) => requireAuth(request),
        // The page drives its filters/search/sort through useSearchParams, and each update is a
        // navigation as far as the router's concerned. Without this, the loader would re-run on
        // every change.
        shouldRevalidate: ({ currentUrl, nextUrl }) => currentUrl.pathname !== nextUrl.pathname,
        errorElement: <RouteErrorPage />,
      },
      { path: 'register', element: <RegisterPage /> },
      { path: 'login', element: <LoginPage /> },
      {
        path: 'comics/:slug',
        element: <ComicDetailPage />,
        loader: comicDetailLoader,
        errorElement: <RouteErrorPage />,
      },
      {
        path: 'moderation',
        element: <ModerationPage />,
        loader: ({ request }) => requireRole('ADMIN', request),
        errorElement: <RouteErrorPage />,
      },
    ],
  },
]);