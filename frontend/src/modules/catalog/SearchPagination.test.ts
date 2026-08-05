import { describe, expect, it } from 'vitest';
import { ELLIPSIS, getPageRange } from './SearchPagination';

describe('getPageRange', () => {
  it('returns every page plainly when they all fit within the available slots', () => {
    expect(getPageRange(1, 5, 2)).toEqual([1, 2, 3, 4, 5]);
    expect(getPageRange(3, 9, 2)).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9]);
  });

  it('does not window a single page', () => {
    expect(getPageRange(1, 1, 2)).toEqual([1]);
  });

  it('extends the left block instead of showing a left ellipsis near the start', () => {
    expect(getPageRange(1, 20, 2)).toEqual([1, 2, 3, 4, 5, 6, 7, ELLIPSIS, 20]);
  });

  it('extends the right block instead of showing a right ellipsis near the end', () => {
    expect(getPageRange(20, 20, 2)).toEqual([1, ELLIPSIS, 14, 15, 16, 17, 18, 19, 20]);
  });

  it('shows both ellipses with the current page centered in the middle', () => {
    expect(getPageRange(10, 20, 2)).toEqual([1, ELLIPSIS, 8, 9, 10, 11, 12, ELLIPSIS, 20]);
  });

  it('flips from an extended left block to a left ellipsis exactly at the boundary', () => {
    // page 5: leftSibling clamps to 3, still adjacent enough to 1 to skip the left ellipsis.
    expect(getPageRange(5, 20, 2)).toEqual([1, 2, 3, 4, 5, 6, 7, ELLIPSIS, 20]);
    // page 6: leftSibling is 4, now far enough from 1 that a left ellipsis makes sense.
    expect(getPageRange(6, 20, 2)).toEqual([1, ELLIPSIS, 4, 5, 6, 7, 8, ELLIPSIS, 20]);
  });

  it('keeps a constant slot count for every current page once windowing kicks in', () => {
    const totalPages = 50;
    const totalSlots = 2 * 2 + 5;
    for (let page = 1; page <= totalPages; page += 1) {
      expect(getPageRange(page, totalPages, 2)).toHaveLength(totalSlots);
    }
  });

  describe('with siblingCount 0 (mobile)', () => {
    it('shows only first, current and last with ellipses between them', () => {
      expect(getPageRange(1, 20, 0)).toEqual([1, 2, 3, ELLIPSIS, 20]);
      expect(getPageRange(10, 20, 0)).toEqual([1, ELLIPSIS, 10, ELLIPSIS, 20]);
      expect(getPageRange(20, 20, 0)).toEqual([1, ELLIPSIS, 18, 19, 20]);
    });

    it('still shows every page plainly when they fit', () => {
      expect(getPageRange(1, 5, 0)).toEqual([1, 2, 3, 4, 5]);
    });

    it('starts windowing as soon as there is one more page than fits', () => {
      expect(getPageRange(1, 6, 0)).toEqual([1, 2, 3, ELLIPSIS, 6]);
    });
  });
});
