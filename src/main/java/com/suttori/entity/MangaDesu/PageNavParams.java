package com.suttori.entity.MangaDesu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PageNavParams {
    private Integer count;
    private Integer page;
    private Integer limit;
    @JsonProperty("order_by")
    private String orderBy;

}
