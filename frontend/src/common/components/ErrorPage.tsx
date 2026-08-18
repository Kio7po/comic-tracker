import { useTranslation } from 'react-i18next';

function messageKey(status: number): string {
  if (status === 404) return 'errors.notFound';
  if (status === 403) return 'errors.notAuthorized';
  return 'errors.generic';
}

function ErrorPage({ status }: Readonly<{ status: number }>) {
  const { t } = useTranslation();

  return (
    <div className="flex flex-col items-center justify-center gap-3 px-6 py-24 text-center">
      <p className="text-9xl font-bold text-muted-foreground">{status}</p>
      <h1 className="text-2xl font-semibold text-foreground">{t('errors.oops')}</h1>
      <p className="text-xl text-foreground">{t('errors.somethingWentWrong')}</p>
      <p className="text-base text-muted-foreground">{t(messageKey(status))}</p>
    </div>
  );
}

export default ErrorPage;