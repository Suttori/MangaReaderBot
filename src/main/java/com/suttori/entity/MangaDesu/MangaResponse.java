package com.suttori.entity.MangaDesu;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MangaResponse {

    @JsonProperty("response")
    private MangaData response;
    @JsonProperty("pageNavParams")
    private PageNavParams pageNavParams;

    public PageNavParams getPageNavParams() {
        return pageNavParams;
    }

    public void setPageNavParams(PageNavParams pageNavParams) {
        this.pageNavParams = pageNavParams;
    }

    public MangaData getResponse() {
        return response;
    }

    public void setResponse(MangaData response) {
        this.response = response;
    }
}

