import { useTranslation } from 'react-i18next';
import { Link, Outlet, useLocation } from 'react-router';
import { ChevronLeft } from 'lucide-react';

function MobileSettingsLayout() {
  const { t } = useTranslation();
  const location = useLocation();
  const isIndexRoute = location.pathname === '/settings';

  return (
    <div className="mx-auto max-w-4xl px-6 py-8">
      {isIndexRoute ? (
        <h1 className="mb-6 text-2xl font-semibold text-foreground">{t('settings.title')}</h1>
      ) : (
        <Link
          to="/settings"
          className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
        >
          <ChevronLeft className="size-4" />
          {t('settings.title')}
        </Link>
      )}
      <Outlet />
    </div>
  );
}

export default MobileSettingsLayout;
