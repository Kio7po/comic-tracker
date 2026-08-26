import { useTranslation } from 'react-i18next';
import { Link, Outlet, useLocation } from 'react-router';
import { Tabs, TabsList, TabsTrigger } from '@/common/components/ui/tabs';
import { Separator } from '@/common/components/ui/separator';
import { SETTINGS_TABS } from './settingsTabs';

function DesktopSettingsLayout() {
  const { t } = useTranslation();
  const location = useLocation();
  const activeTab = SETTINGS_TABS.find((tab) => location.pathname.startsWith(tab.path))?.value ?? SETTINGS_TABS[0].value;

  return (
    <div className="mx-auto max-w-4xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-foreground">{t('settings.title')}</h1>
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
    </div>
  );
}

export default DesktopSettingsLayout;
