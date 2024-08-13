package com.suttori.entity;

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
    private Long mangaDatabaseId;
    private String chapter;
    private String catalogName;

    public NotificationChapterMapping() {
    }

    public NotificationChapterMapping(String mangaId, Long mangaDatabaseId, String chapter, String catalogName) {
        this.mangaId = mangaId;
        this.mangaDatabaseId = mangaDatabaseId;
        this.chapter = chapter;
        this.catalogName = catalogName;
    }

}
