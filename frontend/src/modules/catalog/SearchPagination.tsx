import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from '@/common/components/ui/pagination';
import { cn } from '@/common/lib/utils';
import { MOBILE_QUERY, useMediaQuery } from '@/common/hooks/useMediaQuery';

const SIBLING_COUNT = 2; // amount of siblings at each side a page tries to have (ex. with 5: 2 3 5 6 7)
const MOBILE_SIBLING_COUNT = 0; // below Tailwind's `sm` breakpoint, just show first/current/last
export const ELLIPSIS = 'ellipsis';

export type PageItem = number | typeof ELLIPSIS;

function range(start: number, length: number): number[] {
  return Array.from({ length }, (_, i) => start + i);
}

// Keeps a fixed number of slots (first + ellipsis + middle window + ellipsis + last) regardless
// of where `currentPage` sits, so the control's width never changes as you navigate. Near the
// edges, the middle window extends to fill the slot an ellipsis would otherwise occupy instead
// of just collapsing it, since collapsing alone would shrink the total item count.
export function getPageRange(currentPage: number, totalPages: number, siblingCount: number): PageItem[] {
  const totalSlots = siblingCount * 2 + 5; // n siblings each side + center + 2 ellipsis + start + end

  // if the number of pages is less than the available slots, just show them
  if (totalPages <= totalSlots) {
    return range(1, totalPages);
  }

  // the leftmost and rightmost siblings
  const leftSibling = Math.max(currentPage - siblingCount, 1);
  const rightSibling = Math.min(currentPage + siblingCount, totalPages);

  // show left ellipsis if the (leftmost sibling - 1) is not connected to 1
  // -1 since we have 1 extra slot at each side (the ellipsis slot)
  const showLeftEllipsis = (leftSibling - 1) > 2;
  const showRightEllipsis = rightSibling < totalPages - 2;

  // if we don't show left elipsis, we fill everything at the left (minus ellipsis + end)
  if (!showLeftEllipsis && showRightEllipsis) {
    const leftItemCount = totalSlots - 2;
    return [...range(1, leftItemCount), ELLIPSIS, totalPages];
  }

  if (showLeftEllipsis && !showRightEllipsis) {
    const rightItemCount = totalSlots - 2;
    return [1, ELLIPSIS, ...range(totalPages - rightItemCount + 1, rightItemCount)];
  }

  return [1, ELLIPSIS, ...range(leftSibling, rightSibling - leftSibling + 1), ELLIPSIS, totalPages];
}

interface SearchPaginationProps {
  page: number;
  onPageChange: (page: number) => void;
  totalPages: number | null;
  existMoreItems: boolean;
  className?: string;
}

function SearchPagination({
  page,
  onPageChange,
  totalPages,
  existMoreItems,
  className,
}: Readonly<SearchPaginationProps>) {
  const isMobile = useMediaQuery(MOBILE_QUERY);
  const siblingCount = isMobile ? MOBILE_SIBLING_COUNT : SIBLING_COUNT;

  const isFirstPage = page === 1;
  const isLastPage = totalPages !== null ? page >= totalPages : !existMoreItems;

  function handlePrevious(event: React.MouseEvent<HTMLAnchorElement>) {
    event.preventDefault();
    if (!isFirstPage) {
      onPageChange(page - 1);
    }
  }

  function handleNext(event: React.MouseEvent<HTMLAnchorElement>) {
    event.preventDefault();
    if (!isLastPage) {
      onPageChange(page + 1);
    }
  }

  function handlePageClick(event: React.MouseEvent<HTMLAnchorElement>, target: number) {
    event.preventDefault();
    onPageChange(target);
  }

  const pageItems: PageItem[] = totalPages !== null ? getPageRange(page, totalPages, siblingCount) : [page];

  return (
    <Pagination className={cn('mt-6', className)}>
      <PaginationContent>
        <PaginationItem>
          <PaginationPrevious
            href="#"
            aria-disabled={isFirstPage}
            className={isFirstPage ? 'pointer-events-none opacity-50' : undefined}
            onClick={handlePrevious}
          />
        </PaginationItem>
        {pageItems.map((item, index) =>
          item === ELLIPSIS ? (
            <PaginationItem key={`ellipsis-${index}`}>
              <PaginationEllipsis />
            </PaginationItem>
          ) : (
            <PaginationItem key={item}>
              <PaginationLink
                href="#"
                isActive={item === page}
                onClick={(event) => handlePageClick(event, item)}
              >
                {item}
              </PaginationLink>
            </PaginationItem>
          ),
        )}
        <PaginationItem>
          <PaginationNext
            href="#"
            aria-disabled={isLastPage}
            className={isLastPage ? 'pointer-events-none opacity-50' : undefined}
            onClick={handleNext}
          />
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  );
}

export default SearchPagination;
