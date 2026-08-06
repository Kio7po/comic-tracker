import { useTranslation } from "react-i18next"

function Footer() {
  const { t } = useTranslation();

  return (
    <footer className="mt-auto border-t border-border px-6 py-4 text-sm text-muted-foreground">
      <p>{t('footer.tagline')}</p>
    </footer>
  );
}

export default Footer;