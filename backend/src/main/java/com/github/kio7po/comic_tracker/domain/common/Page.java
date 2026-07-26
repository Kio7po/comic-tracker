package com.github.kio7po.comic_tracker.domain.common;

import java.util.List;

public class Page<T> {
    private List<T> items;
    private boolean existMoreItems;
    
    public Page(List<T> items, boolean existMoreItems) {
        this.items = items;
        this.existMoreItems = existMoreItems;
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

}
