import { Link } from "react-router";
import { useTranslation } from 'react-i18next';

const navLinkClass =
  "relative text-muted-foreground after:absolute after:-bottom-4 after:left-0 after:h-0.5 after:w-full after:origin-left after:scale-x-0 after:bg-foreground after:transition-transform after:duration-100 hover:text-foreground hover:after:scale-x-100";

function Header() {
  const { t } = useTranslation();

  return (
    <header className="sticky top-0 z-50 flex items-center gap-6 border-b border-border bg-background px-6 py-4">
      <Link to="/" className="font-semibold text-foreground">
        Comic Tracker
      </Link>
      <nav className="flex gap-4">
        <Link to="/" className={navLinkClass}>
          {t('nav.home')}
        </Link>
        <Link to="/" className={navLinkClass}>
          {t('nav.browse')}
        </Link>
      </nav>
    </header>
  );
}

export default Header;