import { Link, NavLink, useLocation } from "react-router";
import { useTranslation } from 'react-i18next';
import { BookOpen, Library } from 'lucide-react';
import { useAuth } from '@/common/components/AuthProvider';
import { appendFromParam } from '@/common/lib/authRedirect';
import { buttonVariants } from '@/common/components/ui/button';
import { cn } from '@/common/lib/utils';
import ManganamaoLogo from '@/common/components/ManganamaoLogo';
import UserMenu from './UserMenu';

// Same active-state colors as BottomNavBar's tabs (bg-primary/10 text-primary, with a lighter
// blue swap in dark mode), so "currently selected" reads the same way in both layouts. All tabs
// are always shown (even ones that require a session - the route itself redirects to login), so
// a visibly dimmed tone when logged out is the only hint of which ones will ask for a session,
// instead of hiding them until the user already has an account.
function navLinkClass(isActive: boolean, isDimmed = false) {
  const base = 'flex items-center gap-1.5 rounded-full px-3 py-1.5 transition-colors';
  if (isDimmed) {
    return cn(base, 'text-muted-foreground/50 hover:bg-muted hover:text-muted-foreground');
  }
  return cn(
    base,
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
    <header className="sticky top-0 z-50 flex items-center gap-4 border-b border-border bg-background px-5 py-2 sm:py-4">
      <Link to="/">
        <ManganamaoLogo className="h-10 w-auto" />
      </Link>
      <nav className="hidden gap-3 sm:ml-3 sm:flex">
        <NavLink to="/library" className={({ isActive }) => navLinkClass(isActive, !user)}>
          <Library className="size-4" />
          {t('nav.library')}
        </NavLink>
        <NavLink to="/catalog" className={({ isActive }) => navLinkClass(isActive)}>
          <BookOpen className="size-4" />
          {t('nav.browse')}
        </NavLink>
      </nav>
      {!isLoading &&
        (user ? (
          <UserMenu user={user} />
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