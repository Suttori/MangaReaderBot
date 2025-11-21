package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity(name = "NotificationEntity")
public class NotificationEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mangaId;
    private Long mangaDatabaseId;
    private Long userId;
    private String catalogName;

    public NotificationEntity() {
    }

    public NotificationEntity(String mangaId, Long mangaDatabaseId, Long userId, String catalogName) {
        this.mangaId = mangaId;
        this.mangaDatabaseId = mangaDatabaseId;
        this.userId = userId;
        this.catalogName = catalogName;
    }


}
