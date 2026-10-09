import { Link, NavLink, useLocation } from "react-router";
import { useTranslation } from 'react-i18next';
import { BookOpen, Library } from 'lucide-react';
import { useAuth } from '@/common/components/AuthProvider';
import { appendFromParam } from '@/common/lib/authRedirect';
import { buttonVariants } from '@/common/components/ui/button';
import { cn } from '@/common/lib/utils';
import ManganamaoLogo from '@/common/components/ManganamaoLogo';
import UserDropdownMenu from './UserDropdownMenu';

// Same active-state colors as BottomNavBar's tabs (bg-primary/10 text-primary, with a lighter
// blue swap in dark mode), so "currently selected" reads the same way in both layouts.
function navLinkClass(isActive: boolean) {
  return cn(
    'flex items-center gap-1.5 rounded-full px-3 py-1.5 transition-colors',
    isActive
      ? 'bg-primary/10 font-medium text-primary dark:bg-blue-300/20 dark:text-foreground'
      : 'text-muted-foreground hover:bg-muted hover:text-foreground',
  );
}

function Header() {
  const { t } = useTranslation();
  const { user, isLoading } = useAuth();
  const location = useLocation();

  return (
    <header className="sticky top-0 z-50 flex items-center gap-4 border-b border-border bg-background px-5 py-4">
      <Link to="/">
        <ManganamaoLogo className="h-10 w-auto" />
      </Link>
      <nav className="hidden gap-3 sm:ml-3 sm:flex">
        {user && (
          <NavLink to="/library" className={({ isActive }) => navLinkClass(isActive)}>
            <Library className="size-4" />
            {t('nav.library')}
          </NavLink>
        )}
        <NavLink to="/catalog" className={({ isActive }) => navLinkClass(isActive)}>
          <BookOpen className="size-4" />
          {t('nav.browse')}
        </NavLink>
      </nav>
      {!isLoading &&
        (user ? (
          <UserDropdownMenu user={user} />
        ) : (
          <Link
            to={appendFromParam('/login', location.pathname + location.search)}
            className={buttonVariants({ size: 'sm', className: 'ml-auto' })}
          >
            {t('nav.login')}
          </Link>
        ))}
    </header>
  );
}

export default Header;