import { useTranslation } from 'react-i18next';
import PendingSourcesSection from './PendingSourcesSection';
import PendingEntriesSection from './PendingEntriesSection';

function ModerationPage() {
  const { t } = useTranslation();

  return (
    <div className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-foreground">{t('moderation.title')}</h1>
      <div className="mt-4 flex flex-col gap-6">
        <PendingSourcesSection />
        <PendingEntriesSection />
      </div>
    </div>
  );
}

export default ModerationPage;