package com.suttori.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@Entity(name = "copyMessageManga")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer messageId;
    private Integer backupMessageId;
    private String catalogName;
    private String mangaId;
    private Long mangaDataBaseId;

    @Column(unique = true)
    private String chapterId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_chapter_id", referencedColumnName = "chapterId", foreignKey = @ForeignKey(name = "fk_next_chapter", foreignKeyDefinition = "FOREIGN KEY (next_chapter_id) REFERENCES copy_message_manga(chapter_id) ON DELETE SET NULL"))
    private Chapter nextChapter;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prev_chapter_id", referencedColumnName = "chapterId", foreignKey = @ForeignKey(name = "fk_prev_chapter", foreignKeyDefinition = "FOREIGN KEY (prev_chapter_id) REFERENCES copy_message_manga(chapter_id) ON DELETE SET NULL"))
    private Chapter prevChapter;
    private int orderIndex;
    private String type;
    private String format;
    private String name;
    private String telegraphUrl;
    private String vol;
    private String chapter;
    private String status;
    private Timestamp addedAt;
    private String languageCode;
    private List<String> urlPageList;

    public Chapter() {
    }

    public Chapter(String catalogName, String mangaId, String chapterId, String name, String vol, String chapter, Timestamp addedAt, String format, Long mangaDataBaseId, String languageCode) {
        this.catalogName = catalogName;
        this.mangaId = mangaId;
        this.chapterId = chapterId;
        this.name = name;
        this.vol = vol;
        this.chapter = chapter;
        this.addedAt = addedAt;
        this.format = format;
        this.mangaDataBaseId = mangaDataBaseId;
        this.languageCode = languageCode;
    }

    public Chapter(String mangaId, String name, String vol, String chapter, String status) {
        this.mangaId = mangaId;
        this.name = name;
        this.vol = vol;
        this.chapter = chapter;
        this.status = status;
    }

}
