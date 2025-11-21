package com.suttori.entity.MangaDesu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MangaChapterItem {

    private Long id;
    private String vol;
    private String ch;
    private String title;
    private String date;
    private String check;


}
