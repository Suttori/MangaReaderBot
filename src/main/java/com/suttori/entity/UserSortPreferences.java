package com.suttori.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "UserSortPreference")
@Getter
@Setter
@NoArgsConstructor
public class UserSortPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String catalogName;
    private String sortName;
    private String sortType;

    public UserSortPreferences(Long userId, String catalogName, String sortName, String sortType) {
        this.userId = userId;
        this.catalogName = catalogName;
        this.sortName = sortName;
        this.sortType = sortType;
    }
}
