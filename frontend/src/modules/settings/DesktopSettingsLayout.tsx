import { useTranslation } from 'react-i18next';
import { Link, Outlet, useLocation } from 'react-router';
import PageContainer from '@/common/components/PageContainer';
import PageTitle from '@/common/components/PageTitle';
import { Tabs, TabsList, TabsTrigger } from '@/common/components/ui/tabs';
import { Separator } from '@/common/components/ui/separator';
import { SETTINGS_TABS } from './settingsTabs';

function DesktopSettingsLayout() {
  const { t } = useTranslation();
  const location = useLocation();
  const activeTab = SETTINGS_TABS.find((tab) => location.pathname.startsWith(tab.path))?.value ?? SETTINGS_TABS[0].value;

  return (
    <PageContainer className="max-w-4xl">
      <PageTitle>{t('settings.title')}</PageTitle>
      <Tabs value={activeTab} orientation="vertical" className="mt-6 gap-6">
        <TabsList className="min-w-40 items-stretch">
          {SETTINGS_TABS.map((tab) => (
            <TabsTrigger key={tab.value} value={tab.value} render={<Link to={tab.path} />}>
              <tab.icon className="size-4" />
              {t(tab.labelKey)}
            </TabsTrigger>
          ))}
        </TabsList>
        <Separator orientation="vertical" />
        <div className="flex-1">
          <Outlet />
        </div>
      </Tabs>
    </PageContainer>
  );
}

export default DesktopSettingsLayout;
