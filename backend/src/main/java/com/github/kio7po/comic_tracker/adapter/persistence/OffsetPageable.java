package com.github.kio7po.comic_tracker.adapter.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

// Spring Data's PageRequest always derives its offset as pageNumber * pageSize, so it can't
// represent an arbitrary raw offset exactly. Query execution (row selection, and the count-query
// skip optimization in PageableExecutionUtils) only reads getOffset()/getPageSize()/getSort(), so
// those three are exact here; the page-number-shaped operations below are best-effort only, since
// nothing in this adapter relies on them for a non-page-aligned offset.
final class OffsetPageable implements Pageable {

    private final long offset;
    private final int pageSize;
    private final Sort sort;

    OffsetPageable(long offset, int pageSize, Sort sort) {
        this.offset = offset;
        this.pageSize = pageSize;
        this.sort = sort;
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / pageSize);
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageable(offset + pageSize, pageSize, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetPageable(Math.max(0, offset - pageSize), pageSize, sort) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageable(0, pageSize, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return PageRequest.of(pageNumber, pageSize, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }

}