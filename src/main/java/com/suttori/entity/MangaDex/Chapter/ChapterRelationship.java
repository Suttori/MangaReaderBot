package com.suttori.entity.MangaDex.Chapter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChapterRelationship {

    private String id;
    private String type;
    private String related;
    private ChapterRelationshipAttributes attributes;

}
