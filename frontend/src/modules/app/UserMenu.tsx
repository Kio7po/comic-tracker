import { useNavigate } from 'react-router';
import { LogOut, Settings, ShieldCheck, type LucideIcon } from 'lucide-react';
import { useAuth } from '@/common/components/AuthProvider';
import { MOBILE_QUERY, useMediaQuery } from '@/common/hooks/useMediaQuery';
import UserDrawer from './UserDrawer';
import UserDropdown from './UserDropdown';
import type { UserResponse } from '@/services/user/types';

// The menu's actual content (what items exist, what each one does) lives here, once - UserDropdown
// and UserDrawer only know how to lay a list of these out in their own primitive (DropdownMenuItem
// vs a DrawerClose row), so adding/changing an item never means touching both.
export type UserMenuItem =
  | { kind: 'link'; labelKey: string; icon: LucideIcon; to: string }
  | { kind: 'action'; labelKey: string; icon: LucideIcon; onClick: () => void; destructive?: boolean };

// A dropdown's small, closely-spaced items don't translate well to touch, so mobile gets its own
// Drawer-based component instead of one component branching internally - they share close to
// nothing presentation-wise (dropdown: DropdownMenu primitives; drawer: Drawer primitives), same
// reasoning as SettingsLayout/DesktopSettingsLayout/MobileSettingsLayout.
function UserMenu({ user }: Readonly<{ user: UserResponse }>) {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const isMobile = useMediaQuery(MOBILE_QUERY);

  async function handleLogout() {
    await logout();
    void navigate('/');
  }

  const items: UserMenuItem[] = [
    { kind: 'link', labelKey: 'nav.settings', icon: Settings, to: '/settings' },
    ...(user.role === 'ADMIN'
      ? [{ kind: 'link', labelKey: 'nav.moderation', icon: ShieldCheck, to: '/moderation' } as const]
      : []),
    { kind: 'action', labelKey: 'nav.logout', icon: LogOut, onClick: () => void handleLogout(), destructive: true },
  ];

  return isMobile ? <UserDrawer user={user} items={items} /> : <UserDropdown user={user} items={items} />;
}

export default UserMenu;
