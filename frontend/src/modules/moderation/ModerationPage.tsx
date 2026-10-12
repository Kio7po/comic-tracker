import { useTranslation } from 'react-i18next';
import PageContainer from '@/common/components/PageContainer';
import PageTitle from '@/common/components/PageTitle';
import PendingSourcesSection from './PendingSourcesSection';
import PendingEntriesSection from './PendingEntriesSection';

function ModerationPage() {
  const { t } = useTranslation();

  return (
    <PageContainer className="max-w-3xl">
      <PageTitle>{t('moderation.title')}</PageTitle>
      <div className="mt-4 flex flex-col gap-6">
        <PendingSourcesSection />
        <PendingEntriesSection />
      </div>
    </PageContainer>
  );
}

export default ModerationPage;