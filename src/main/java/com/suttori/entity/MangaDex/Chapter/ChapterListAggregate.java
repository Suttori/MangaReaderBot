package com.suttori.entity.MangaDex.Chapter;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ChapterListAggregate {

    private String result;
    private Map<String, Volume> volumes;

}
