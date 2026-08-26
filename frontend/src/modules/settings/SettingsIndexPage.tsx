import { useTranslation } from 'react-i18next';
import { Link, Navigate } from 'react-router';
import { ChevronRight } from 'lucide-react';
import { MOBILE_QUERY, useMediaQuery } from '@/common/hooks/useMediaQuery';
import { SETTINGS_TABS } from './settingsTabs';

function SettingsIndexPage() {
  const { t } = useTranslation();
  const isMobile = useMediaQuery(MOBILE_QUERY);

  // Desktop always shows the sidebar and content together, so bare /settings collapses straight
  // to the first tab. Only mobile, which can't show both at once, actually needs this as its own
  // screen.
  if (!isMobile) {
    return <Navigate to="profile" replace />;
  }

  return (
    <nav className="flex flex-col divide-y divide-border rounded-lg border border-border">
      {SETTINGS_TABS.map((tab) => (
        <Link key={tab.value} to={tab.path} className="flex items-center gap-3 px-4 py-3 text-sm hover:bg-muted">
          <tab.icon className="size-4 text-muted-foreground" />
          <span className="flex-1 text-foreground">{t(tab.labelKey)}</span>
          <ChevronRight className="size-4 text-muted-foreground" />
        </Link>
      ))}
    </nav>
  );
}

export default SettingsIndexPage;
