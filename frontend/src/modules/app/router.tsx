import { createBrowserRouter } from 'react-router';
import Layout from '@/common/components/Layout';
import RouteErrorPage from '@/common/components/RouteErrorPage';
import type { Comic } from '@/services/comic/types';
import SearchPage from '@/modules/catalog/SearchPage';
import LibraryPage from '@/modules/library/LibraryPage';
import ComicDetailPage, { comicDetailLoader } from '@/modules/comic/ComicDetailPage';
import RegisterPage from '@/modules/auth/RegisterPage';
import LoginPage from '@/modules/auth/LoginPage';
import ModerationPage from '@/modules/moderation/ModerationPage';
import SettingsLayout from '@/modules/settings/SettingsLayout';
import SettingsIndexPage from '@/modules/settings/SettingsIndexPage';
import ProfileSettingsPage from '@/modules/settings/ProfileSettingsPage';
import AppearanceSettingsPage from '@/modules/settings/AppearanceSettingsPage';
import { requireAuth, requireRole } from './routeGuards';
import Home from './Home';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'catalog', element: <SearchPage />, handle: { title: 'catalog.title' } },
      {
        path: 'library',
        element: <LibraryPage />,
        loader: ({ request }) => requireAuth(request),
        // The page drives its filters/search/sort through useSearchParams, and each update is a
        // navigation as far as the router's concerned. Without this, the loader would re-run on
        // every change.
        shouldRevalidate: ({ currentUrl, nextUrl }) => currentUrl.pathname !== nextUrl.pathname,
        errorElement: <RouteErrorPage />,
        handle: { title: 'library.title' },
      },
      { path: 'register', element: <RegisterPage />, handle: { title: 'auth.register.title' } },
      { path: 'login', element: <LoginPage />, handle: { title: 'auth.login.title' } },
      {
        path: 'comics/:slug',
        element: <ComicDetailPage />,
        loader: comicDetailLoader,
        errorElement: <RouteErrorPage />,
        handle: { title: (data: unknown) => (data as Comic).title },
      },
      {
        path: 'moderation',
        element: <ModerationPage />,
        loader: ({ request }) => requireRole('ADMIN', request),
        errorElement: <RouteErrorPage />,
        handle: { title: 'moderation.title' },
      },
      {
        path: 'settings',
        id: 'settings',
        element: <SettingsLayout />,
        // Every settings tab needs the same auth guard, so it lives once on this parent route.
        // Tabs read the resolved user via useRouteLoaderData('settings') instead of their own loader.
        loader: ({ request }) => requireAuth(request),
        errorElement: <RouteErrorPage />,
        handle: { title: 'settings.title' },
        children: [
          { index: true, element: <SettingsIndexPage /> },
          {
            path: 'profile',
            element: <ProfileSettingsPage />,
            handle: { title: 'settings.tabs.profile' },
          },
          {
            path: 'appearance',
            element: <AppearanceSettingsPage />,
            handle: { title: 'settings.tabs.appearance' },
          },
        ],
      },
    ],
  },
]);