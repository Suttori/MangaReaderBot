package com.suttori.entity.MangaDex.Chapter;

import com.suttori.entity.MangaDex.Manga.MangaAttributes;
import com.suttori.entity.MangaDex.Manga.MangaDataMangaDex;
import com.suttori.entity.MangaDex.Manga.MangaRelationship;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChapterListResponse {

    private String result;
    private String response;
    private List<ChapterData> data;
    private int limit;
    private int offset;
    private int total;

}
