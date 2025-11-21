package com.suttori.entity.MangaDesu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;


import java.util.List;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MangaDataAsSearchResult {

    private Long id;
    private String name;
    private String russian;
    private String kind;
    private MangaImage image;
    private String url;
    private String reading;
    private Integer ongoing;
    private Integer anons;
    private Integer adult;
    private String status;
    private Long aired_on;
    private Long released_on;
    private Double score;
    private Integer score_users;
    private Integer views;
    private String description;
    private Long checked;
    private Long updated;
    private String genres;
    private List<MangaTranslator> translators;
    private String synonyms;
    private Long thread_id;
    private Long shikimori_id;
    private Long myanimelist_id;
    private String mangadex_id;
    private MangaChapters chapters;
    private String age_limit;
    private String trans_status;
    private String licensed;

}
