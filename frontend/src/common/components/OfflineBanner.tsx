import { useTranslation } from 'react-i18next';
import { WifiOff } from 'lucide-react';
import { useOnlineStatus } from '@/common/hooks/useOnlineStatus';

// Thin strip shown only while the browser reports no connection. The app shell is precached by the
// service worker so it still opens offline, but nothing from the API is, so most screens won't
// load their data. <output> is a live region (implicit role="status") and stays mounted, empty
// when online, so a screen reader announces the message when it appears; a live region inserted
// together with its content often isn't. `className` goes on it, the actual child of the layout.
function OfflineBanner({ className }: Readonly<{ className?: string }>) {
  const { t } = useTranslation();
  const isOnline = useOnlineStatus();

  return (
    <output className={className}>
      {!isOnline && (
        <div className="flex items-center justify-center gap-2 border-b border-zinc-700 bg-zinc-800 px-4 py-1 text-xs font-medium text-zinc-100 dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-300">
          <WifiOff className="size-3.5 shrink-0" aria-hidden="true" />
          {t('offline.message')}
        </div>
      )}
    </output>
  );
}

export default OfflineBanner;
