package com.suttori.entity.MangaDex.Chapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Volume {


    private String volume;
    private int count;
    private Map<String, ChapterMangaDex> chapters;

}
