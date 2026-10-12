import type { ReactNode } from 'react';
import { cn } from '@/common/lib/utils';

// Shared outer wrapper for a top-level page (Library, Browse, Moderation, Settings...), less
// vertical padding on mobile. max-w-* stays the caller's own concern via className - it already
// varies page to page.
function PageContainer({ children, className }: Readonly<{ children: ReactNode; className?: string }>) {
  return <div className={cn('mx-auto px-6 py-5 sm:py-8', className)}>{children}</div>;
}

export default PageContainer;
