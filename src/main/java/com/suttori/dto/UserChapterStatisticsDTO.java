package com.suttori.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UserChapterStatisticsDTO {


    private Long userId;
    private Long totalChapters;

    public UserChapterStatisticsDTO() {
    }

    public UserChapterStatisticsDTO(Long userId, Long totalChapters) {
        this.userId = userId;
        this.totalChapters = totalChapters;
    }
}
