package com.suttori.entity.MangaDex.Tag;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TagResponse {

    private String result;
    private String response;
    private List<TagData> data;
    private int limit;
    private int offset;
    private int total;

}
