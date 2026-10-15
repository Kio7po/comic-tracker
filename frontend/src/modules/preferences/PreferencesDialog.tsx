import { Navigate, useLocation, useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import AppearanceFields from '@/common/components/AppearanceFields';
import { MOBILE_QUERY, useMediaQuery } from '@/common/hooks/useMediaQuery';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/common/components/ui/dialog';
import { PREFERENCES_HASH } from './preferences';

// Device-level preferences (theme, language) reachable without a session. Open state lives in the
// URL fragment, so the dialog is linkable and the back button closes it.
function PreferencesDialog() {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const isMobile = useMediaQuery(MOBILE_QUERY);
  const isOpen = location.hash === PREFERENCES_HASH;

  // Mobile has a screen of its own instead of a dialog; a link to the dialog (e.g. one shared from
  // desktop) lands there too.
  if (isMobile) {
    return isOpen ? <Navigate to="/preferences" replace /> : null;
  }

  function handleOpenChange(open: boolean) {
    if (!open) {
      // The user may have landed here directly from a link. Since the close button tries to
      // do navigate(-1) by default to close the dialog, in this case it wouldn't close since
      // there is nothing to go back to.
      void navigate({ pathname: location.pathname, search: location.search, hash: '' }, { replace: true });
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="text-lg">{t('preferences.title')}</DialogTitle>
          <DialogDescription>{t('preferences.description')}</DialogDescription>
        </DialogHeader>
        <div className='mb-4 mt-2'>
          <AppearanceFields />
        </div>
      </DialogContent>
    </Dialog>
  );
}

export default PreferencesDialog;
