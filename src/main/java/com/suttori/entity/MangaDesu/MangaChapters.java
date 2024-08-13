package com.suttori.entity.MangaDesu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MangaChapters {

    private MangaChapterDesu first;
    private MangaChapterDesu last;
    private MangaChapterDesu updated;
    private Integer count;
    private List<MangaChapterItem> list;

}
