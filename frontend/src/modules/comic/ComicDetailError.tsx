import { useTranslation } from 'react-i18next';

function ComicDetailError() {
  const { t } = useTranslation();

  return <p className="px-6 py-16 text-center text-sm text-destructive">{t('detail.loadError')}</p>;
}

export default ComicDetailError;
