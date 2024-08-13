package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity(name = "StatisticEntity")
@Getter
@Setter
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


}
