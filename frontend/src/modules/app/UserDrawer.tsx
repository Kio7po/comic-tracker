import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import { displayNameInitials } from '@/common/lib/displayNameInitials';
import { cn } from '@/common/lib/utils';
import { Avatar, AvatarFallback, AvatarImage } from '@/common/components/ui/avatar';
import { Drawer, DrawerClose, DrawerContent, DrawerHeader, DrawerTitle, DrawerTrigger } from '@/common/components/ui/drawer';
import type { UserMenuItem } from './UserMenu';
import type { UserResponse } from '@/services/user/types';

// Full-width, generously padded rows - the touch-friendly substitute for UserDropdown's small,
// closely-spaced DropdownMenuItems. No text color baked in here - items.destructive decides it,
// see itemClass below. Appending a second text-* color on top of this one isn't reliable:
// Tailwind's generated stylesheet order (not the order classes appear in the string) decides
// which wins when two utilities set the same property.
const baseItemClass = 'flex items-center gap-3 rounded-md px-3 py-3 text-base hover:bg-muted';

function itemClass(item: UserMenuItem) {
  return cn(baseItemClass, item.kind === 'action' && item.destructive ? 'text-destructive' : 'text-foreground');
}

function UserDrawer({ user, items }: Readonly<{ user: UserResponse; items: UserMenuItem[] }>) {
  const { t } = useTranslation();

  return (
    // swipeDirection="right": trigger lives in Header's top-right, so the sheet enters from there.
    <Drawer swipeDirection="right">
      <DrawerTrigger className="ml-auto flex items-center rounded-full p-1 outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50">
        <Avatar>
          <AvatarImage src={user.pictureUrl ?? undefined} alt="" />
          <AvatarFallback>{displayNameInitials(user.displayName)}</AvatarFallback>
        </Avatar>
      </DrawerTrigger>
      <DrawerContent>
        <DrawerHeader className="items-center text-center">
          <Avatar className="size-12">
            <AvatarImage src={user.pictureUrl ?? undefined} alt="" />
            <AvatarFallback>{displayNameInitials(user.displayName)}</AvatarFallback>
          </Avatar>
          <DrawerTitle className="text-base font-normal text-muted-foreground">
            {t('nav.loggedInAs')} <span className="font-semibold text-foreground">{user.displayName}</span>
          </DrawerTitle>
        </DrawerHeader>
        {/* Wrapped in DrawerClose (not a plain Link, unlike e.g. LibraryComicCard's own drawer)
            because this component stays mounted across navigation - it lives in Header, not a
            page that unmounts on route change - so the drawer would otherwise stay open over
            whatever page comes next. DrawerClose's `render` composes its own close handler with
            the rendered element's own behavior, so a link item still navigates. */}
        <div className="flex flex-col gap-1 px-4 pt-4 pb-4">
          {items.map((item) => {
            if (item.kind === 'link') {
              return (
                <DrawerClose key={item.labelKey} render={<Link to={item.to} />} className={itemClass(item)}>
                  <item.icon className="size-5" />
                  {t(item.labelKey)}
                </DrawerClose>
              );
            }
            return (
              <DrawerClose
                key={item.labelKey}
                render={<button type="button" onClick={item.onClick} />}
                className={itemClass(item)}
              >
                <item.icon className="size-5" />
                {t(item.labelKey)}
              </DrawerClose>
            );
          })}
        </div>
      </DrawerContent>
    </Drawer>
  );
}

export default UserDrawer;
