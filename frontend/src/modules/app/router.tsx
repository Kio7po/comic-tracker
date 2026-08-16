import { createBrowserRouter } from 'react-router';
import Layout from '@/common/components/Layout';
import RouteErrorPage from '@/common/components/RouteErrorPage';
import SearchPage from '@/modules/catalog/SearchPage';
import ComicDetailPage, { comicDetailLoader } from '@/modules/comic/ComicDetailPage';
import RegisterPage from '@/modules/auth/RegisterPage';
import LoginPage from '@/modules/auth/LoginPage';
import ModerationPage from '@/modules/moderation/ModerationPage';
import { requireRole } from './routeGuards';
import Home from './Home';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'catalog', element: <SearchPage /> },
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