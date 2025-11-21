package com.suttori.entity.MangaDex.Chapter;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class ChapterUrl {

    private String hash;
    private List<String> data;
    private List<String> dataSaver;

}
