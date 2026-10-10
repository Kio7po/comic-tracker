import { NavLink } from 'react-router';
import { useTranslation } from 'react-i18next';
import { BookOpen, Library } from 'lucide-react';
import { useAuth } from '@/common/components/AuthProvider';
import { cn } from '@/common/lib/utils';

// Same icons as Header.tsx's nav links, so the bottom bar's tabs read as the visual shorthand
// for those same labelled links rather than a separate, unrelated vocabulary. requiresAuth tabs
// are still always shown (see Header.tsx's own comment on navLinkClass for why) - just dimmed
// when logged out, same as there.
const tabs = [
  { to: '/library', icon: Library, labelKey: 'nav.library', requiresAuth: true },
  { to: '/catalog', icon: BookOpen, labelKey: 'nav.browse', requiresAuth: false },
] as const;

// The background lives on a `before:` pseudo-element, not this span directly, so it alone can
// scale-x on activation (a quick horizontal expand) without squashing the icon with it.
function tabIconClass(isActive: boolean, isDimmed: boolean) {
  const base =
    "relative flex items-center justify-center rounded-full px-5 py-1 transition-colors before:absolute before:inset-0 before:scale-x-0 before:rounded-full before:transition-transform before:duration-100 before:content-['']";
  if (isDimmed) {
    return cn(base, 'text-muted-foreground/50');
  }
  if (isActive) {
    return cn(base, 'text-primary before:scale-x-100 before:bg-primary/10 dark:text-foreground dark:before:bg-blue-300/20');
  }
  return cn(base, 'text-muted-foreground');
}

// Outside the pill on purpose - the pill above is the selection indicator, this label is a
// separate, always-visible element below it. truncate in case a future longer label ever needs
// it; current labels are short enough that it won't fire.
function tabLabelClass(isActive: boolean, isDimmed: boolean) {
  const base = 'max-w-full truncate text-sm';
  if (isDimmed) {
    return cn(base, 'text-muted-foreground/50');
  }
  if (isActive) {
    return cn(base, 'font-medium text-primary dark:text-foreground');
  }
  return cn(base, 'text-muted-foreground');
}

// Mobile-only "app-style" tab bar (Android/iOS bottom navigation convention), replacing the
// Header's own nav links below the sm breakpoint.
// No "Home" tab: going home is the logo's job (Header), not a tab among equals here.
function BottomNavBar() {
  const { t } = useTranslation();
  const { user } = useAuth();

  return (
    <nav
      aria-label={t('nav.mainNavigation')}
      className="fixed inset-x-0 bottom-0 z-50 flex h-16 items-stretch border-t border-border bg-background pb-[env(safe-area-inset-bottom)] sm:hidden"
    >
      {tabs.map(({ to, icon: Icon, labelKey, requiresAuth }) => {
        const isDimmed = requiresAuth && !user;

        return (
          <NavLink key={to} to={to} className="flex flex-1 flex-col items-center justify-center gap-0.5 px-1">
            {({ isActive }) => (
              <>
                <span className={tabIconClass(isActive, isDimmed)}>
                  <Icon className="size-6" />
                </span>
                <span className={tabLabelClass(isActive, isDimmed)}>{t(labelKey)}</span>
              </>
            )}
          </NavLink>
        );
      })}
    </nav>
  );
}

export default BottomNavBar;
