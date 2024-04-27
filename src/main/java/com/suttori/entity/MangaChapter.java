package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "copyMessageManga")
public class MangaChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer messageId;
    private Integer backupMessageId;
    private String mangaId;
    private String type;
    private String name;
    private String telegraphUrl;
    private int vol;
    private int chapter;
    private String status;

    public MangaChapter() {
    }

    public MangaChapter(String mangaId, String name, int vol, int chapter, String status) {
        this.mangaId = mangaId;
        this.name = name;
        this.vol = vol;
        this.chapter = chapter;
        this.status = status;
    }

}
