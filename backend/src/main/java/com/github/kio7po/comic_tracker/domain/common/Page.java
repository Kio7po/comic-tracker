package com.github.kio7po.comic_tracker.domain.common;

import java.util.List;

import org.jspecify.annotations.Nullable;

public class Page<T> {
    private List<T> items;
    private boolean existMoreItems;
    private @Nullable Integer totalItems;

    public Page(List<T> items, boolean existMoreItems, @Nullable Integer totalItems) {
        this.items = items;
        this.existMoreItems = existMoreItems;
        this.totalItems = totalItems;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public boolean isExistMoreItems() {
        return existMoreItems;
    }

    public void setExistMoreItems(boolean existMoreItems) {
        this.existMoreItems = existMoreItems;
    }

    public @Nullable Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(@Nullable Integer totalItems) {
        this.totalItems = totalItems;
    }

}
