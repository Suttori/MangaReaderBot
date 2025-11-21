package com.suttori.entity.MangaDesu;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MangaChapterDesu {

    private String vol;
    private String ch;
    private String name;
    private String date;

}
