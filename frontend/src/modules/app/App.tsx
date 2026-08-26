import { RouterProvider } from 'react-router';
import { AuthProvider } from '@/common/components/AuthProvider';
import { ThemeProvider } from '@/common/components/ThemeProvider';
import { Toaster } from '@/common/components/ui/toast';
import { router } from './router';

function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <RouterProvider router={router} />
        <Toaster />
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;
