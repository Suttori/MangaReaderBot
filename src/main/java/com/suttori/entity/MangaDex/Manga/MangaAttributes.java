package com.suttori.entity.MangaDex.Manga;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class MangaAttributes {

    private Map<String, String> title;
    private List<Map<String, String>> altTitles;
    @JsonDeserialize(using = SafeDescriptionDeserializer.class)
    private Map<String, String> description;
    private Boolean isLocked;
    private Map<String, String> links;
    private String originalLanguage;
    private String lastVolume;
    private String lastChapter;
    private String publicationDemographic;
    private String status;
    private int year;
    private String contentRating;
    private boolean chapterNumbersResetOnNewVolume;
    private List<String> availableTranslatedLanguages;
    private String latestUploadedChapter;
    private List<MangaTag> tags;
    private String state;
    private int version;
    private String createdAt;
    private String updatedAt;
    private String officialLinks;

}
