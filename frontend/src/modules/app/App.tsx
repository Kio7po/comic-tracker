import { RouterProvider } from 'react-router';
import { AuthProvider } from '@/common/components/AuthProvider';
import { Toaster } from '@/common/components/ui/toast';
import { router } from './router';

function App() {
  return (
    <AuthProvider>
      <RouterProvider router={router} />
      <Toaster />
    </AuthProvider>
  );
}

export default App;
