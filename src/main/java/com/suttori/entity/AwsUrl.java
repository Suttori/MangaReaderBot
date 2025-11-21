package com.suttori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.common.aliasing.qual.Unique;

import java.sql.Timestamp;

@Entity(name = "awsUrl")
@Getter
@Setter
public class AwsUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long chapterDataBaseId;
    private String awsUrl;
    private int height;
    private int width;
    private Long fileSize;
    private Long userId;
    private Timestamp addedAt;

    public AwsUrl() {
    }

    public AwsUrl(Long chapterDataBaseId, String awsUrl, int height, int width, Long fileSize, Long userId, Timestamp addedAt) {
        this.chapterDataBaseId = chapterDataBaseId;
        this.awsUrl = awsUrl;
        this.height = height;
        this.width = width;
        this.fileSize = fileSize;
        this.userId = userId;
        this.addedAt = addedAt;
    }
}
