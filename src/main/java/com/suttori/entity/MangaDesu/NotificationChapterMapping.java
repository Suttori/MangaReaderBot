package com.suttori.entity.MangaDesu;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "NotificationChapterMapping")
public class NotificationChapterMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mangaId;
    private String chapter;
    private String catalogName;

    public NotificationChapterMapping() {
    }

    public NotificationChapterMapping(String mangaId, String chapter, String catalogName) {
        this.mangaId = mangaId;
        this.chapter = chapter;
        this.catalogName = catalogName;
    }

}
