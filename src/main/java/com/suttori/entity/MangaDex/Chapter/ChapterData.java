package com.suttori.entity.MangaDex.Chapter;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChapterData {

    private String id;
    private String type;
    private ChapterAttributes attributes;
    private List<ChapterRelationship> relationships;

}
