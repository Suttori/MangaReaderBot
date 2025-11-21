package com.suttori.entity.MangaDex.Manga;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class MangaDataMangaDex {

    private String id;
    private String type;
    private MangaAttributes attributes;
    private List<MangaRelationship> relationships;

}
