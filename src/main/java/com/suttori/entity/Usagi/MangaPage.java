package com.suttori.entity.Usagi;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MangaPage {

    public String url;
    public String height;
    public String width;
    public String size;

    public MangaPage(String url, String height, String width, String size) {
        this.url = url;
        this.height = height;
        this.width = width;
        this.size = size;
    }
}
