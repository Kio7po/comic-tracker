import { createBrowserRouter } from 'react-router';
import Layout from '@/common/components/Layout';
import SearchPage from '@/modules/catalog/SearchPage';
import ComicDetailPage, { comicDetailLoader } from '@/modules/comic/ComicDetailPage';
import ComicDetailError from '@/modules/comic/ComicDetailError';
import RegisterPage from '@/modules/auth/RegisterPage';
import Home from './Home';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'catalog', element: <SearchPage /> },
      { path: 'register', element: <RegisterPage /> },
      {
        path: 'comics/:slug',
        element: <ComicDetailPage />,
        loader: comicDetailLoader,
        errorElement: <ComicDetailError />,
      },
    ],
  },
]);
