import { Link, useLocation } from 'react-router';
import { useTranslation } from 'react-i18next';
import { Settings } from 'lucide-react';
import { buttonVariants } from '@/common/components/ui/button';
import { MOBILE_QUERY, useMediaQuery } from '@/common/hooks/useMediaQuery';
import { cn } from '@/common/lib/utils';
import { PREFERENCES_HASH } from './preferences';

// Gear shown to logged-out visitors, opening the preferences dialog over the current page.
// Search is kept explicitly: a hash-only `to` would drop the current query string.
function PreferencesButton() {
  const { t } = useTranslation();
  const location = useLocation();
  const isMobile = useMediaQuery(MOBILE_QUERY);

  return (
    <Link
      to={
        isMobile
          ? '/preferences'
          : { pathname: location.pathname, search: location.search, hash: PREFERENCES_HASH }
      }
      aria-label={t('preferences.title')}
      title={t('preferences.title')}
      className={cn(buttonVariants({ variant: 'ghost', size: 'icon' }), 'rounded-full')}
    >
      <Settings className="size-5" />
    </Link>
  );
}

export default PreferencesButton;
