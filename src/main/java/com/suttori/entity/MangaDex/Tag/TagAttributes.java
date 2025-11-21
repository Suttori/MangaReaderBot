package com.suttori.entity.MangaDex.Tag;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class TagAttributes {
    private Map<String, String> name;
    private Map<String, String> description;
    private String group;
    private int version;
}
