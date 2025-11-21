package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity(name = "ReadStatus")
@Getter
@Setter
public class ReadStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String mangaId;
    private String chapterId;
    private Long userId;
    private Timestamp updateAt;
    private String catalogName;


    public ReadStatus() {
    }

    public ReadStatus(String mangaId, String chapterId, Long userId, Timestamp updateAt, String catalogName) {
        this.mangaId = mangaId;
        this.chapterId = chapterId;
        this.userId = userId;
        this.updateAt = updateAt;
        this.catalogName = catalogName;
    }
}
