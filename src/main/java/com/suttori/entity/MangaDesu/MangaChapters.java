package com.suttori.entity.MangaDesu;

import java.util.List;

public class MangaChapters {

    private MangaChapter first;
    private MangaChapter last;
    private MangaChapter updated;
    private Integer count;
    private List<MangaChapterItem> list;

    public MangaChapter getFirst() {
        return first;
    }

    public void setFirst(MangaChapter first) {
        this.first = first;
    }

    public MangaChapter getLast() {
        return last;
    }

    public void setLast(MangaChapter last) {
        this.last = last;
    }

    public MangaChapter getUpdated() {
        return updated;
    }

    public void setUpdated(MangaChapter updated) {
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
