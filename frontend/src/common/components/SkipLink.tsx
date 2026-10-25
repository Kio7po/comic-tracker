import type { MouseEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { cn } from '@/common/lib/utils';

// First focusable element on the page, hidden until a keyboard user tabs to it, so they can jump
// past the header/nav straight to the content. It moves focus with JS instead of following its
// href, so no fragment is left in the URL (fragments drive dialogs here, see PreferencesDialog);
// the href only makes it a focusable link. The target needs tabIndex={-1} to accept focus.
function SkipLink({ targetId }: Readonly<{ targetId: string }>) {
  const { t } = useTranslation();

  function handleClick(event: MouseEvent<HTMLAnchorElement>) {
    event.preventDefault();
    document.getElementById(targetId)?.focus();
  }

  return (
    <a
      href={`#${targetId}`}
      onClick={handleClick}
      className={cn(
        'sr-only focus:not-sr-only',
        'focus:fixed focus:top-2 focus:left-2 focus:z-[60]',
        'focus:rounded-md focus:bg-primary focus:px-4 focus:py-2 focus:text-sm focus:font-medium focus:text-primary-foreground',
      )}
    >
      {t('nav.skipToContent')}
    </a>
  );
}

export default SkipLink;
