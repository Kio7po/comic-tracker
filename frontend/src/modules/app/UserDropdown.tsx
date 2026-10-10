import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
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
import type { UserMenuItem } from './UserMenu';
import type { UserResponse } from '@/services/user/types';

function UserDropdown({ user, items }: Readonly<{ user: UserResponse; items: UserMenuItem[] }>) {
  const { t } = useTranslation();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="ml-auto flex items-center gap-2 rounded-full p-1 pr-3 outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50">
        <Avatar>
          <AvatarImage src={user.pictureUrl ?? undefined} alt="" />
          <AvatarFallback>{displayNameInitials(user.displayName)}</AvatarFallback>
        </Avatar>
        <span className="text-sm font-medium text-foreground">{user.displayName}</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuGroup>
          <DropdownMenuLabel>
            {t('nav.loggedInAs')} <span className="font-semibold text-foreground">{user.displayName}</span>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          {items.map((item) => {
            if (item.kind === 'link') {
              return (
                <DropdownMenuItem key={item.labelKey} render={<Link to={item.to} />}>
                  <item.icon />
                  {t(item.labelKey)}
                </DropdownMenuItem>
              );
            }
            return (
              <DropdownMenuItem
                key={item.labelKey}
                variant={item.destructive ? 'destructive' : undefined}
                onClick={item.onClick}
              >
                <item.icon />
                {t(item.labelKey)}
              </DropdownMenuItem>
            );
          })}
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

export default UserDropdown;
