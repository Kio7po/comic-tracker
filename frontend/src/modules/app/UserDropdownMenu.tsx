import { Link, useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { LogOut, Settings, ShieldCheck } from 'lucide-react';
import { useAuth } from '@/common/components/AuthProvider';
import { displayNameInitials } from '@/common/lib/displayNameInitials';
import { Avatar, AvatarFallback, AvatarImage } from '@/common/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/common/components/ui/dropdown-menu';
import type { UserResponse } from '@/services/user/types';

function UserDropdownMenu({ user }: Readonly<{ user: UserResponse }>) {
  const { t } = useTranslation();
  const { logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate('/');
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="ml-auto flex items-center gap-2 rounded-full p-1 pr-3 outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50">
        <Avatar>
          <AvatarImage src={user.pictureUrl ?? undefined} alt="" />
          <AvatarFallback>{displayNameInitials(user.displayName)}</AvatarFallback>
        </Avatar>
        <span className="hidden text-sm font-medium text-foreground sm:inline">{user.displayName}</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuGroup>
          <DropdownMenuLabel>
            {t('nav.loggedInAs')} <span className="font-semibold text-foreground">{user.displayName}</span>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem render={<Link to="/settings" />}>
            <Settings />
            {t('nav.settings')}
          </DropdownMenuItem>
          {user.role === 'ADMIN' && (
            <DropdownMenuItem render={<Link to="/moderation" />}>
              <ShieldCheck />
              {t('nav.moderation')}
            </DropdownMenuItem>
          )}
          <DropdownMenuItem variant="destructive" onClick={handleLogout}>
            <LogOut />
            {t('nav.logout')}
          </DropdownMenuItem>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

export default UserDropdownMenu;