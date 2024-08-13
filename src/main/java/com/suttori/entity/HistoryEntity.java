package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity(name = "HistoryEntity")
@Setter
@Getter
public class HistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String mangaId;
    private Long mangaDatabaseId;
    private Long userId;
    private String name;
    private String russian;
    private Timestamp updateAt;
    private String catalogName;

    public HistoryEntity() {
    }

    public HistoryEntity(String mangaId, Long mangaDatabaseId, Long userId, String name, String russian, Timestamp updateAt, String catalogName) {
        this.mangaId = mangaId;
        this.userId = userId;
        this.name = name;
        this.russian = russian;
        this.updateAt = updateAt;
        this.catalogName = catalogName;
        this.mangaDatabaseId = mangaDatabaseId;
    }

}
