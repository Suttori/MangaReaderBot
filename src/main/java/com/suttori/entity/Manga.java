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
    private Integer coverMessageId;
    private Integer backupCoverMessageId;
    private String mangaId;
    private String catalogName;

    private String name;
    private String type;
    private String status;
    private String genres;
    private String description;
    private int releaseDate;
    private String rating;
    private int numberOfChapters;


}
