import { useTranslation } from 'react-i18next';
import { Link, Outlet, useLocation } from 'react-router';
import { ChevronLeft } from 'lucide-react';
import PageContainer from '@/common/components/PageContainer';
import PageTitle from '@/common/components/PageTitle';

function MobileSettingsLayout() {
  const { t } = useTranslation();
  const location = useLocation();
  const isIndexRoute = location.pathname === '/settings';

  return (
    <PageContainer className="max-w-4xl">
      {isIndexRoute ? (
        <PageTitle className="mb-3">{t('settings.title')}</PageTitle>
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
    </PageContainer>
  );
}

export default MobileSettingsLayout;
