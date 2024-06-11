package com.suttori.entity.MangaDesu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MangaChapters {

    private MangaChapterDesu first;
    private MangaChapterDesu last;
    private MangaChapterDesu updated;
    private Integer count;
    private List<MangaChapterItem> list;

    public MangaChapterDesu getFirst() {
        return first;
    }

    public void setFirst(MangaChapterDesu first) {
        this.first = first;
    }

    public MangaChapterDesu getLast() {
        return last;
    }

    public void setLast(MangaChapterDesu last) {
        this.last = last;
    }

    public MangaChapterDesu getUpdated() {
        return updated;
    }

    public void setUpdated(MangaChapterDesu updated) {
        this.updated = updated;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<MangaChapterItem> getList() {
        return list;
    }

    public void setList(List<MangaChapterItem> list) {
        this.list = list;
    }
}
