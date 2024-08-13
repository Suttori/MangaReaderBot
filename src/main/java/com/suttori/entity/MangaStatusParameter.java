package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity(name = "MangaStatusParameter")
@Getter
@Setter
public class MangaStatusParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String mangaId;
    private Long mangaDatabaseId;
    private Long userId;
    private String status;
    private String name;
    private String russian;
    private Timestamp addedAt;
    private String catalogName;

    public MangaStatusParameter() {
    }

    public MangaStatusParameter(String mangaId, Long mangaDatabaseId, Long userId, String status, String name, String russian, Timestamp addedAt, String catalogName) {
        this.mangaId = mangaId;
        this.mangaDatabaseId = mangaDatabaseId;
        this.userId = userId;
        this.status = status;
        this.name = name;
        this.russian = russian;
        this.addedAt = addedAt;
        this.catalogName = catalogName;
    }

}
