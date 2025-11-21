package com.suttori.entity.MangaDex.Tag;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TagData {

    private String id;
    private String type;
    private TagAttributes attributes;
    private List<Object> relationships;
}
