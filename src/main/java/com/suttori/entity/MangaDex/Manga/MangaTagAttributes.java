package com.suttori.entity.MangaDex.Manga;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class MangaTagAttributes {

    private Map<String, String> name;
    @JsonDeserialize(using = SafeDescriptionDeserializer.class)
    private Map<String, String> description;
    private String group;
    private String version;

}
