import { createBrowserRouter } from 'react-router';
import Layout from '@/common/components/Layout';
import SearchPage from '@/modules/catalog/SearchPage';
import Home from './Home';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'catalog', element: <SearchPage /> },
    ],
  },
]);
