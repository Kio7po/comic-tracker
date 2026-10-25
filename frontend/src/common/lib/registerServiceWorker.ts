import { registerSW } from 'virtual:pwa-register';
import i18n from '@/common/i18n';
import { toast } from '@/common/components/ui/toast';

// Registers the service worker and, when a new version has been downloaded and is waiting, asks
// the user to reload instead of swapping it in on its own (registerType: 'prompt' in
// vite.config.ts), so an open form isn't lost to a surprise refresh.
export function registerServiceWorker() {
  const updateServiceWorker = registerSW({
    onNeedRefresh() {
      toast.add({
        title: i18n.t('pwa.updateAvailable'),
        description: i18n.t('pwa.updateDescription'),
        type: 'info',
        // 0 = doesn't auto-dismiss: unlike the other toasts this one is waiting on the user.
        timeout: 0,
        actionProps: {
          children: i18n.t('pwa.reload'),
          onClick: () => void updateServiceWorker(true),
        },
      });
    },
  });
}
