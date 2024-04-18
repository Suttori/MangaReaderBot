package com.suttori.entity.MangaDesu;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PageNavParams {
    private Integer count;
    private Integer page;
    private Integer limit;
    @JsonProperty("order_by")
    private String orderBy;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }
}
