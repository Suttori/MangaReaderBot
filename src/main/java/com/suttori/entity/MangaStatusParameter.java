package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.sql.Timestamp;

@Entity(name = "MangaStatusParameter")
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


    public Long getMangaDatabaseId() {
        return mangaDatabaseId;
    }

    public void setMangaDatabaseId(Long mangaDatabaseId) {
        this.mangaDatabaseId = mangaDatabaseId;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    public String getRussian() {
        return russian;
    }

    public void setRussian(String russian) {
        this.russian = russian;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Timestamp addedAt) {
        this.addedAt = addedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMangaId() {
        return mangaId;
    }

    public void setMangaId(String mangaId) {
        this.mangaId = mangaId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
