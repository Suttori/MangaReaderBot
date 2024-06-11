package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

import java.sql.Timestamp;

@Entity(name = "StatisticEntity")
public class StatisticEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String mangaId;
    private Long userId;
    private String name;
    private String russian;
    private String vol;
    private String ch;
    @Getter
    private String catalogName;
    private Timestamp addedAt;


    public StatisticEntity() {
    }

    public StatisticEntity(String mangaId, Long userId, String name, String russian, String vol, String ch, Timestamp addedAt, String catalogName) {
        this.mangaId = mangaId;
        this.userId = userId;
        this.name = name;
        this.russian = russian;
        this.vol = vol;
        this.ch = ch;
        this.addedAt = addedAt;
        this.catalogName = catalogName;
    }

    public void setVol(String vol) {
        this.vol = vol;
    }

    public void setCh(String ch) {
        this.ch = ch;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRussian() {
        return russian;
    }

    public void setRussian(String russian) {
        this.russian = russian;
    }

    public String getVol() {
        return vol;
    }

    public String getCh() {
        return ch;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public Timestamp getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Timestamp addedAt) {
        this.addedAt = addedAt;
    }
}
