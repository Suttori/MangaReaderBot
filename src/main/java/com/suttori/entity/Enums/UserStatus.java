package com.suttori.entity.Enums;

import lombok.Getter;

@Getter
public enum UserStatus {
    NEWBIE("Новичок"),
    STUDENT("Ученик"),
    ADVANCED("Продвинутый"),
    EXPERT("Эксперт"),
    MASTER("Мастер"),
    VETERAN("Ветеран"),
    GURU("Гуру"),
    CHAMPION("Чемпион"),
    IDEAL("Идеал"),
    LEGEND("Легенда"),
    DEITY("Божество");

    private final String statusName;

    UserStatus(String statusName) {
        this.statusName = statusName;
    }

}


