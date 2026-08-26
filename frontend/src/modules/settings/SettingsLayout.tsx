import { MOBILE_QUERY, useMediaQuery } from '@/common/hooks/useMediaQuery';
import MobileSettingsLayout from './MobileSettingsLayout';
import DesktopSettingsLayout from './DesktopSettingsLayout';

// Mobile can't show the tab list and its content at once, so each gets its own layout instead of
// one component branching internally - they share close to nothing (mobile: title-or-back-link +
// content; desktop: sidebar + separator + content).
function SettingsLayout() {
  const isMobile = useMediaQuery(MOBILE_QUERY);
  return isMobile ? <MobileSettingsLayout /> : <DesktopSettingsLayout />;
}

export default SettingsLayout;
