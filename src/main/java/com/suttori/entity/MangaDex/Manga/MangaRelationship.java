package com.suttori.entity.MangaDex.Manga;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MangaRelationship {

    private String id;
    private String type;
    private String related;
    private MangaRelationshipAttributes attributes;

}
