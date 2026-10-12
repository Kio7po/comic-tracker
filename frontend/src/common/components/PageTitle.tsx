import type { ReactNode } from 'react';
import { cn } from '@/common/lib/utils';

// Shared h1 for a page's own title (Library, Browse, Moderation...), smaller on mobile.
// Margin stays the caller's own concern via className - it already varies page to page.
function PageTitle({ children, className }: Readonly<{ children: ReactNode; className?: string }>) {
  return <h1 className={cn('text-xl font-semibold text-foreground sm:text-2xl', className)}>{children}</h1>;
}

export default PageTitle;
