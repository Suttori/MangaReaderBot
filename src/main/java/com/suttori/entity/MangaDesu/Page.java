package com.suttori.entity.MangaDesu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.telegram.telegraph.api.objects.Node;

import java.util.List;

@Getter
@Setter
public class Page {

    @JsonProperty("path")
    private String path;
    @JsonProperty("url")
    private String url;
    @JsonProperty("title")
    private String title;
    @JsonProperty("description")
    private String description;
    @JsonProperty("author_name")
    private String authorName;
    @JsonProperty("author_url")
    private String authorUrl;
    @JsonProperty("image_url")
    private String imageUrl;
    @JsonProperty("content")
    private List<Node> content;
    @JsonProperty("views")
    private Integer views;
    @JsonProperty("can_edit")
    private Boolean canEdit;


}
