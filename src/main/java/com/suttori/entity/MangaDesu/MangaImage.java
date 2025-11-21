package com.suttori.entity.MangaDesu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MangaImage {

    private String original;
    private String preview;
    private String x120;
    private String x225;
    private String x48;
    private String x32;

}
