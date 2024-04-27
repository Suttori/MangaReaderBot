package com.suttori.entity.MangaDesu;

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
    private Long userId;

    public NotificationEntity() {
    }

    public NotificationEntity(String mangaId, Long userId) {
        this.mangaId = mangaId;
        this.userId = userId;
    }


}
