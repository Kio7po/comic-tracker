import { Outlet } from 'react-router';
import Header from '@/app/Header';
import Footer from '@/app/Footer';

function Layout() {
  return (
    <div className="flex min-h-svh flex-col">
      <Header/>
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer/>
    </div>
  );
}

export default Layout;
