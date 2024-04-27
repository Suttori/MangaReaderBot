package com.suttori.entity.MangaDex.Manga;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class MangaTag {

    private String id;
    private String type;
    private MangaTagAttributes attributes;
    private List<MangaRelationship> relationships;

}
