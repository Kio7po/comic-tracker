import { Outlet } from 'react-router';
import Header from '@/app/Header';
import Footer from '@/app/Footer';
import { TooltipProvider } from '@/common/components/ui/tooltip';

function Layout() {
  return (
    <TooltipProvider>
      <div className="flex min-h-svh flex-col">
        <Header/>
        <main className="flex-1">
          <Outlet />
        </main>
        <Footer/>
      </div>
    </TooltipProvider>
  );
}

export default Layout;
