package com.suttori.entity.MangaDesu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MangaPages {

    private MangaPageInfo ch_curr;
    private MangaPageInfo ch_prev;
    private MangaPageInfo ch_next;
    private List<MangaPage> list;

    public MangaPageInfo getCh_curr() {
        return ch_curr;
    }

    public void setCh_curr(MangaPageInfo ch_curr) {
        this.ch_curr = ch_curr;
    }

    public MangaPageInfo getCh_prev() {
        return ch_prev;
    }

    public void setCh_prev(MangaPageInfo ch_prev) {
        this.ch_prev = ch_prev;
    }

    public MangaPageInfo getCh_next() {
        return ch_next;
    }

    public void setCh_next(MangaPageInfo ch_next) {
        this.ch_next = ch_next;
    }

    public List<MangaPage> getList() {
        return list;
    }

    public void setList(List<MangaPage> list) {
        this.list = list;
    }
}
