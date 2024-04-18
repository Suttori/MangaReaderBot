package com.suttori.entity.MangaDesu;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity(name = "copyMessageManga")
public class CopyMessageManga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer messageId;
    private Integer backupMessageId;
    private Long mangaId;
    private String type;
    private String name;
    private String telegraphUrl;
    private int vol;
    private int chapter;
    private String status;

    public CopyMessageManga() {
    }

    public CopyMessageManga(Long mangaId, String name, int vol, int chapter, String status) {
        this.mangaId = mangaId;
        this.name = name;
        this.vol = vol;
        this.chapter = chapter;
        this.status = status;
    }

    public Integer getBackupMessageId() {
        return backupMessageId;
    }

    public void setBackupMessageId(Integer backupMessageId) {
        this.backupMessageId = backupMessageId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public String getTelegraphUrl() {
        return telegraphUrl;
    }

    public void setTelegraphUrl(String telegraphUrl) {
        this.telegraphUrl = telegraphUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMangaId() {
        return mangaId;
    }

    public void setMangaId(Long manhwaId) {
        this.mangaId = manhwaId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVol() {
        return vol;
    }

    public void setVol(int vol) {
        this.vol = vol;
    }

    public int getChapter() {
        return chapter;
    }

    public void setChapter(int chapter) {
        this.chapter = chapter;
    }
}
