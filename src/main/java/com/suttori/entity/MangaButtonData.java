package com.suttori.entity;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MangaButtonData {

    private Long userId;
    private String mangaId;
    private Long mangaDatabaseId;
    private String languageCode;

    public MangaButtonData(Long userId, String mangaId, Long mangaDatabaseId) {
        this.userId = userId;
        this.mangaId = mangaId;
        this.mangaDatabaseId = mangaDatabaseId;
    }

    public MangaButtonData(Long userId, String mangaId, Long mangaDatabaseId, String languageCode) {
        this.userId = userId;
        this.mangaId = mangaId;
        this.mangaDatabaseId = mangaDatabaseId;
        this.languageCode = languageCode;
    }
}
