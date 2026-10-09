import { Outlet } from 'react-router';
import Header from '@/modules/app/Header';
import Footer from '@/modules/app/Footer';
import BottomNavBar from '@/modules/app/BottomNavBar';
import useDocumentTitle from '@/common/hooks/useDocumentTitle';
import { TooltipProvider } from '@/common/components/ui/tooltip';

function Layout() {
  useDocumentTitle();

  return (
    <TooltipProvider>
      {/* pb-* reserves space for BottomNavBar's fixed height (h-16) plus its own safe-area
          inset, on mobile only, so it doesn't end up covering the page's trailing content
          (including Footer) the way a fixed-position element otherwise would. */}
      <div className="flex min-h-svh flex-col pb-[calc(4rem+env(safe-area-inset-bottom))] sm:pb-0">
        <Header/>
        <main className="flex-1">
          <Outlet />
        </main>
        <Footer/>
      </div>
      <BottomNavBar />
    </TooltipProvider>
  );
}

export default Layout;
