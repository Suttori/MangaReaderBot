package com.suttori.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFileSizeDto {

    private Long userId;
    private Double totalSizeMb;

    public UserFileSizeDto(Long userId, Double totalSizeMb) {
        this.userId = userId;
        this.totalSizeMb = totalSizeMb;
    }
}
