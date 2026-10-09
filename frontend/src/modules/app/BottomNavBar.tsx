import { NavLink } from 'react-router';
import { useTranslation } from 'react-i18next';
import { BookOpen, Library } from 'lucide-react';
import { cn } from '@/common/lib/utils';

// Same icons as Header.tsx's nav links, so the bottom bar's tabs read as the visual shorthand
// for those same labelled links rather than a separate, unrelated vocabulary.
const tabs = [
  { to: '/library', icon: Library, labelKey: 'nav.library' },
  { to: '/catalog', icon: BookOpen, labelKey: 'nav.browse' },
] as const;

// Mobile-only "app-style" tab bar (Android/iOS bottom navigation convention), replacing the
// Header's own nav links below the sm breakpoint - see Layout.tsx for the matching bottom
// padding that keeps page content (and the footer) from ending up underneath this fixed bar.
// No "Home" tab: going home is the logo's job (Header), not a tab among equals here.
function BottomNavBar() {
  const { t } = useTranslation();

  return (
    <nav
      aria-label={t('nav.mainNavigation')}
      className="fixed inset-x-0 bottom-0 z-50 flex h-16 items-stretch border-t border-border bg-background pb-[env(safe-area-inset-bottom)] sm:hidden"
    >
      {tabs.map(({ to, icon: Icon, labelKey }) => (
        <NavLink key={to} to={to} className="flex flex-1 flex-col items-center justify-center gap-0.5 px-1">
          {({ isActive }) => (
            <>
              <span
                className={cn(
                  'flex items-center justify-center rounded-full px-5 py-1 transition-colors',
                  isActive ? 'bg-primary/10 text-primary dark:bg-blue-300/20 dark:text-foreground' : 'text-muted-foreground',
                )}
              >
                <Icon className="size-6" />
              </span>
              {/* Outside the pill on purpose - the pill is the selection indicator, the label
                  is a separate, always-visible element below it. truncate in case a future
                  longer label ever needs it; current labels are short enough that it won't fire. */}
              <span
                className={cn(
                  'max-w-full truncate text-sm',
                  isActive ? 'font-medium text-primary dark:text-foreground' : 'text-muted-foreground',
                )}
              >
                {t(labelKey)}
              </span>
            </>
          )}
        </NavLink>
      ))}
    </nav>
  );
}

export default BottomNavBar;
