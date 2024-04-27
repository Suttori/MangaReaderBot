package com.suttori.entity.MangaDex.Manga;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class MangaTagAttributes {

    private Map<String, String> name;
    private Map<String, String> description;
    private String group;
    private String version;

}
