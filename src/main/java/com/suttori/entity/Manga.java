package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "manga")
public class Manga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String coverFileId;
    private String coverUrl;
    private String mangaId;
    private String catalogName;

    private String name;
    private String type;
    private String status;
    private String genres;
    private String format;
    private String description;
    private String releaseDate;
    private String rating;
    private String numberOfChapters;
    private String languageCode;

    public Manga() {
    }

    public Manga(String coverUrl, String mangaId, String catalogName, String name, String type, String status, String genres, String description, String releaseDate, String rating, String numberOfChapters, String format, String languageCode) {
        this.coverUrl = coverUrl;
        this.mangaId = mangaId;
        this.catalogName = catalogName;
        this.name = name;
        this.type = type;
        this.status = status;
        this.genres = genres;
        this.description = description;
        this.releaseDate = releaseDate;
        this.rating = rating;
        this.numberOfChapters = numberOfChapters;
        this.format = format;
        this.languageCode = languageCode;
    }
}
