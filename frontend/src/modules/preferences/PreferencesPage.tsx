import { useTranslation } from 'react-i18next';
import { Navigate } from 'react-router';
import { useAuth } from '@/common/components/AuthProvider';
import AppearanceFields from '@/common/components/AppearanceFields';
import PageContainer from '@/common/components/PageContainer';
import PageTitle from '@/common/components/PageTitle';
import { MOBILE_QUERY, useMediaQuery } from '@/common/hooks/useMediaQuery';
import { PREFERENCES_HASH } from './preferences';

// Mobile presentation of the preferences, which on desktop are a dialog (PreferencesDialog) - this
// route is the canonical URL for both, so anywhere else it collapses to where those preferences
// actually live.
function PreferencesPage() {
  const { t } = useTranslation();
  const { user, isLoading } = useAuth();
  const isMobile = useMediaQuery(MOBILE_QUERY);

  // Don't branch on `user` before the session restore resolves, or a logged-in user would be
  // briefly treated as a visitor.
  if (isLoading) {
    return null;
  }
  // With a session, theme and language already live in the account's settings.
  if (user) {
    return <Navigate to="/settings/appearance" replace />;
  }
  // Desktop shows these as a dialog over the home page instead of a screen of their own.
  if (!isMobile) {
    return <Navigate to={{ pathname: '/', hash: PREFERENCES_HASH }} replace />;
  }

  return (
    <PageContainer className="max-w-4xl">
      <PageTitle className="mb-3">{t('preferences.title')}</PageTitle>
      <AppearanceFields />
    </PageContainer>
  );
}

export default PreferencesPage;
