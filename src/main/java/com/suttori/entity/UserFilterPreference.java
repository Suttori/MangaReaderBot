package com.suttori.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "UserFilterPreference")
@Getter
@Setter
@NoArgsConstructor
public class UserFilterPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String catalogName;
    private String filterType;
    private String filterTag;
    private String filterName;
    private String filterValue;

    public UserFilterPreference(Long userId, String catalogName, String filterType, String filterTag, String filterName, String filterValue) {
        this.userId = userId;
        this.catalogName = catalogName;
        this.filterType = filterType;
        this.filterTag = filterTag;
        this.filterName = filterName;
        this.filterValue = filterValue;
    }
}
