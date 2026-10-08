import { Link, useLocation } from "react-router";
import { useTranslation } from 'react-i18next';
import { useAuth } from '@/common/components/AuthProvider';
import { appendFromParam } from '@/common/lib/authRedirect';
import { buttonVariants } from '@/common/components/ui/button';
import ManganamaoLogo from '@/common/components/ManganamaoLogo';
import UserDropdownMenu from './UserDropdownMenu';

const navLinkClass =
  "relative text-muted-foreground after:absolute after:-bottom-4 after:left-0 after:h-0.5 after:w-full after:origin-left after:scale-x-0 after:bg-foreground after:transition-transform after:duration-100 hover:text-foreground hover:after:scale-x-100";

function Header() {
  const { t } = useTranslation();
  const { user, isLoading } = useAuth();
  const location = useLocation();

  return (
    <header className="sticky top-0 z-50 flex items-center gap-4 border-b border-border bg-background px-5 py-4">
      <Link to="/">
        <ManganamaoLogo className="h-10 w-auto" />
      </Link>
      <nav className="flex gap-4">
        <Link to="/catalog" className={navLinkClass}>
          {t('nav.browse')}
        </Link>
        {user && (
          <Link to="/library" className={navLinkClass}>
            {t('nav.library')}
          </Link>
        )}
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