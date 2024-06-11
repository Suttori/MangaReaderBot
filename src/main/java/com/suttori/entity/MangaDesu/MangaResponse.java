package com.suttori.entity.MangaDesu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MangaResponse {

    @JsonProperty("response")
    private MangaDataDesu response;
    @JsonProperty("pageNavParams")
    private PageNavParams pageNavParams;

    public PageNavParams getPageNavParams() {
        return pageNavParams;
    }

    public void setPageNavParams(PageNavParams pageNavParams) {
        this.pageNavParams = pageNavParams;
    }

    public MangaDataDesu getResponse() {
        return response;
    }

    public void setResponse(MangaDataDesu response) {
        this.response = response;
    }
}

