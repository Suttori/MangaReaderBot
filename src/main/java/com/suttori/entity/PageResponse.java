package com.suttori.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PageResponse {

    @JsonProperty("result")
    private Page result;

    public Page getResult() {
        return result;
    }

    public void setResult(Page result) {
        this.result = result;
    }
}
