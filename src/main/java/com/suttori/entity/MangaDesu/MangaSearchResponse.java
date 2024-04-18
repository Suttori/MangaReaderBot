package com.suttori.entity.MangaDesu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MangaSearchResponse {

    @JsonProperty("response")
    private List<MangaDataAsSearchResult> response;
    @JsonProperty("pageNavParams")
    private PageNavParams pageNavParams;

    public List<MangaDataAsSearchResult> getResponse() {
        return response;
    }

    public void setResponse(List<MangaDataAsSearchResult> response) {
        this.response = response;
    }

    public PageNavParams getPageNavParams() {
        return pageNavParams;
    }

    public void setPageNavParams(PageNavParams pageNavParams) {
        this.pageNavParams = pageNavParams;
    }
}

