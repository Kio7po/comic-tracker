import { beforeEach, describe, expect, it, vi } from 'vitest';
import { registerSW } from 'virtual:pwa-register';
import { toast } from '@/common/components/ui/toast';
import { registerServiceWorker } from './registerServiceWorker';

vi.mock('virtual:pwa-register', () => ({ registerSW: vi.fn() }));
vi.mock('@/common/components/ui/toast', () => ({ toast: { add: vi.fn() } }));

describe('registerServiceWorker', () => {
  const updateServiceWorker = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(registerSW).mockReturnValue(updateServiceWorker);
  });

  function triggerNeedRefresh() {
    registerServiceWorker();
    vi.mocked(registerSW).mock.calls[0][0]?.onNeedRefresh?.();
  }

  it('does not show anything until a new version is waiting', () => {
    registerServiceWorker();

    expect(registerSW).toHaveBeenCalledOnce();
    expect(toast.add).not.toHaveBeenCalled();
  });

  it('shows a persistent info toast when a new version is waiting', () => {
    triggerNeedRefresh();

    expect(toast.add).toHaveBeenCalledOnce();
    expect(toast.add).toHaveBeenCalledWith(expect.objectContaining({ type: 'info', timeout: 0 }));
  });

  it('reloads into the new version only when the user presses the toast action', () => {
    triggerNeedRefresh();
    expect(updateServiceWorker).not.toHaveBeenCalled();

    const { actionProps } = vi.mocked(toast.add).mock.calls[0][0];
    actionProps?.onClick?.({} as never);

    expect(updateServiceWorker).toHaveBeenCalledWith(true);
  });
});
