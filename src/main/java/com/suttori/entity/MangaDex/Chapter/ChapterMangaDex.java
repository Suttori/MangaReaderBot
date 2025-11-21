package com.suttori.entity.MangaDex.Chapter;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChapterMangaDex {

    private String chapter;
    private String id;
    private List<String> others;
    private int count;
    private Boolean isUnavailable;

}
