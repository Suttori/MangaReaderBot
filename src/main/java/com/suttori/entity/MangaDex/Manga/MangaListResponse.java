package com.suttori.entity.MangaDex.Manga;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Setter
@Getter
public class MangaListResponse {

    private String result;
    private String response;
    private List<MangaDataMangaDex> data;
    private int limit;
    private int offset;
    private int total;

}
